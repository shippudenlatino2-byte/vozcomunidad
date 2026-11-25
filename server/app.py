import os
import hmac
import hashlib
import sqlite3
import time
import base64
from contextlib import contextmanager
from datetime import datetime, timedelta
from io import BytesIO
from uuid import uuid4
from zoneinfo import ZoneInfo

from flask import Flask, Response, jsonify, redirect, request, session
from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter
from pathlib import Path

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 6 * 1024 * 1024
CLAVE_PANEL = os.environ.get("CLAVE_PANEL", "").strip()
app.secret_key = os.environ.get("CLAVE_SECRETA") or hashlib.sha256(
    b"vozcomunidad|" + CLAVE_PANEL.encode("utf-8")
).hexdigest()
app.config["SESSION_COOKIE_HTTPONLY"] = True
app.config["SESSION_COOKIE_SAMESITE"] = "Lax"
app.config["SESSION_COOKIE_SECURE"] = os.environ.get("RENDER", "").lower() == "true"
app.config["PERMANENT_SESSION_LIFETIME"] = timedelta(hours=12)

MESES = "ene feb mar abr may jun jul ago sep oct nov dic".split()


def momento_local():
    ahora = datetime.now(ZoneInfo("America/Bogota"))
    hora = ahora.strftime("%I:%M").lstrip("0")
    periodo = "a. m." if ahora.hour < 12 else "p. m."
    return f"{ahora.day} {MESES[ahora.month - 1]}, {hora} {periodo}"


def campo(valor, limite, multilinea=False):
    if not isinstance(valor, str):
        return ""
    limpio = valor.strip()
    if not multilinea:
        limpio = " ".join(limpio.split())
    return limpio[:limite]


class Almacen:
    def __init__(self):
        self.url = os.environ.get("DATABASE_URL", "").strip()
        if self.url.startswith("postgres://"):
            self.url = "postgresql://" + self.url[len("postgres://"):]
        self.postgres = bool(self.url)
        self.ruta = os.path.join("data", "voz.db")

    def conectar(self):
        if self.postgres:
            import psycopg
            from psycopg.rows import dict_row

            return psycopg.connect(self.url, row_factory=dict_row)
        os.makedirs("data", exist_ok=True)
        conexion = sqlite3.connect(self.ruta)
        conexion.row_factory = sqlite3.Row
        return conexion

    @contextmanager
    def sesion(self):
        conexion = self.conectar()
        try:
            yield conexion
            conexion.commit()
        except Exception:
            conexion.rollback()
            raise
        finally:
            conexion.close()

    def preparar(self):
        tipo_flag = "BOOLEAN" if self.postgres else "INTEGER"
        sql = f"""
            CREATE TABLE IF NOT EXISTS respuestas (
                id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                telefono TEXT NOT NULL,
                barrio TEXT NOT NULL,
                opinion TEXT NOT NULL,
                momento TEXT NOT NULL,
                creado_en TEXT NOT NULL,
                tiene_audio {tipo_flag} NOT NULL,
                duracion_seg INTEGER NOT NULL,
                estado TEXT NOT NULL,
                tono TEXT NOT NULL,
                acento TEXT NOT NULL,
                urgente {tipo_flag} NOT NULL,
                audio {"BYTEA" if self.postgres else "BLOB"}
            )
        """
        with self.sesion() as conexion:
            conexion.execute(sql)
            self.asegurar_audio(conexion)
            for nombre in ("canal", "direccion", "localidad", "categoria", "concejal"):
                self.asegurar_texto(conexion, nombre)
            conexion.execute(
                """
                CREATE TABLE IF NOT EXISTS concejales (
                    id TEXT PRIMARY KEY,
                    nombre TEXT NOT NULL,
                    cedula TEXT NOT NULL,
                    telefono TEXT NOT NULL,
                    partido TEXT NOT NULL,
                    tarjeton TEXT NOT NULL,
                    creado_en TEXT NOT NULL
                )
                """
            )
            conexion.execute("CREATE UNIQUE INDEX IF NOT EXISTS concejales_cedula ON concejales (cedula)")
            conexion.execute(
                """
                CREATE TABLE IF NOT EXISTS lideres (
                    id TEXT PRIMARY KEY,
                    nombre TEXT NOT NULL,
                    cedula TEXT NOT NULL,
                    telefono TEXT NOT NULL,
                    correo TEXT NOT NULL,
                    barrio TEXT NOT NULL,
                    candidato_id TEXT NOT NULL,
                    candidato TEXT NOT NULL,
                    creado_en TEXT NOT NULL
                )
                """
            )
            conexion.execute("CREATE UNIQUE INDEX IF NOT EXISTS lideres_cedula ON lideres (cedula)")

    def asegurar_audio(self, conexion):
        if "audio" in self.columnas(conexion):
            return
        tipo = "BYTEA" if self.postgres else "BLOB"
        conexion.execute(f"ALTER TABLE respuestas ADD COLUMN audio {tipo}")

    def asegurar_texto(self, conexion, nombre):
        if nombre in self.columnas(conexion):
            return
        conexion.execute(f"ALTER TABLE respuestas ADD COLUMN {nombre} TEXT")

    def columnas(self, conexion):
        if self.postgres:
            filas = conexion.execute(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'respuestas'"
            ).fetchall()
            return {fila["column_name"] for fila in filas}
        filas = conexion.execute("PRAGMA table_info(respuestas)").fetchall()
        return {fila["name"] for fila in filas}

    def listar(self):
        consulta = """
            SELECT id, nombre, telefono, barrio, direccion, localidad, canal, categoria, concejal,
                   opinion, momento, creado_en, tiene_audio, duracion_seg, estado, tono,
                   acento, urgente
            FROM respuestas
            ORDER BY creado_en DESC
        """
        with self.sesion() as conexion:
            filas = conexion.execute(consulta).fetchall()
        return [fila_a_json(fila) for fila in filas]

    def guardar(self, datos):
        nombre = campo(datos.get("nombre"), 80)
        telefono = campo(datos.get("telefono"), 30)
        opinion = campo(datos.get("opinion"), 2000, multilinea=True)
        if not nombre or not telefono or not opinion:
            raise ValueError("Completa nombre, teléfono y la opinión")
        try:
            duracion = int(datos.get("duracionSeg") or 0)
        except (TypeError, ValueError):
            duracion = 0
        duracion = max(0, min(180, duracion))
        contenido = decodificar_audio(datos.get("audio"))
        tiene_audio = contenido is not None
        registro = {
            "id": uuid4().hex,
            "nombre": nombre,
            "telefono": telefono,
            "barrio": campo(datos.get("barrio"), 80),
            "direccion": campo(datos.get("direccion"), 160),
            "localidad": campo(datos.get("localidad"), 80),
            "canal": campo(datos.get("canal"), 40),
            "categoria": campo(datos.get("categoria"), 40),
            "concejal": campo(datos.get("concejal"), 80),
            "opinion": opinion,
            "momento": momento_local(),
            "creado_en": datetime.now(ZoneInfo("UTC")).isoformat(),
            "tiene_audio": tiene_audio,
            "duracion_seg": duracion if tiene_audio else 0,
            "estado": "Enviada a la comunidad",
            "tono": "EXITO",
            "acento": "VERDE",
            "urgente": False,
            "audio": contenido,
        }
        marca = "%s" if self.postgres else "?"
        columnas = list(registro.keys())
        sql = (
            f"INSERT INTO respuestas ({', '.join(columnas)}) "
            f"VALUES ({', '.join(marca for _ in columnas)})"
        )
        with self.sesion() as conexion:
            conexion.execute(sql, tuple(registro.values()))
        return fila_a_json(registro)

    def leer_audio(self, identificador):
        if not isinstance(identificador, str) or len(identificador) != 32:
            return None
        marca = "%s" if self.postgres else "?"
        with self.sesion() as conexion:
            fila = conexion.execute(
                f"SELECT audio FROM respuestas WHERE id = {marca}",
                (identificador,),
            ).fetchone()
        if fila is None or fila["audio"] is None:
            return None
        return bytes(fila["audio"])

    def eliminar(self, identificadores):
        validos = []
        for identificador in identificadores:
            if not isinstance(identificador, str) or len(identificador) != 32:
                continue
            if not all(letra in "0123456789abcdef" for letra in identificador):
                continue
            validos.append(identificador)
        if not validos:
            return 0
        marca = "%s" if self.postgres else "?"
        consulta = f"DELETE FROM respuestas WHERE id IN ({', '.join(marca for _ in validos)})"
        with self.sesion() as conexion:
            cursor = conexion.execute(consulta, tuple(validos))
            return cursor.rowcount

    def listar_concejales(self):
        with self.sesion() as conexion:
            filas = conexion.execute(
                """
                SELECT id, nombre, cedula, telefono, partido, tarjeton, creado_en
                FROM concejales
                ORDER BY nombre
                """
            ).fetchall()
        return [concejal_a_json(fila) for fila in filas]

    def guardar_concejal(self, datos):
        nombre = campo(datos.get("nombre"), 80)
        cedula = "".join(letra for letra in campo(datos.get("cedula"), 20) if letra.isdigit())
        telefono = campo(datos.get("telefono"), 30)
        partido = campo(datos.get("partido"), 80)
        tarjeton = "".join(letra for letra in campo(datos.get("tarjeton"), 10) if letra.isdigit())
        if not nombre or len(cedula) < 5 or not telefono or not partido or not tarjeton:
            raise ValueError("Completa nombre, cédula, teléfono, partido y número de tarjetón")
        marca = "%s" if self.postgres else "?"
        with self.sesion() as conexion:
            repetida = conexion.execute(
                f"SELECT id FROM concejales WHERE cedula = {marca}",
                (cedula,),
            ).fetchone()
            if repetida is not None:
                raise ValueError("Ya existe un concejal con esa cédula")
            registro = {
                "id": uuid4().hex,
                "nombre": nombre,
                "cedula": cedula,
                "telefono": telefono,
                "partido": partido,
                "tarjeton": tarjeton,
                "creado_en": datetime.now(ZoneInfo("UTC")).isoformat(),
            }
            columnas = list(registro.keys())
            sql = (
                f"INSERT INTO concejales ({', '.join(columnas)}) "
                f"VALUES ({', '.join(marca for _ in columnas)})"
            )
            conexion.execute(sql, tuple(registro.values()))
        return concejal_a_json(registro)

    def listar_lideres(self):
        with self.sesion() as conexion:
            filas = conexion.execute(
                """
                SELECT id, nombre, cedula, telefono, correo, barrio, candidato_id, candidato, creado_en
                FROM lideres
                ORDER BY nombre
                """
            ).fetchall()
        return [lider_a_json(fila) for fila in filas]

    def guardar_lider(self, datos):
        nombre = campo(datos.get("nombre"), 120)
        cedula = "".join(letra for letra in campo(datos.get("cedula"), 20) if letra.isdigit())
        telefono = campo(datos.get("telefono"), 30)
        correo = correo_valido(datos.get("correo"))
        barrio = campo(datos.get("barrio"), 80)
        candidato_id = campo(datos.get("candidato_id"), 32)
        if campo(datos.get("correo"), 120) and not correo:
            raise ValueError("Escribe un correo válido")
        if not nombre or len(cedula) < 5 or not telefono or not correo or not barrio or not candidato_id:
            raise ValueError("Completa nombre, cédula, teléfono, correo, barrio y el candidato")
        candidato = next((item for item in self.listar_concejales() if item["id"] == candidato_id), None)
        if candidato is None:
            raise ValueError("Elige un candidato creado en la pestaña Concejales")
        marca = "%s" if self.postgres else "?"
        with self.sesion() as conexion:
            repetida = conexion.execute(
                f"SELECT id FROM lideres WHERE cedula = {marca}",
                (cedula,),
            ).fetchone()
            if repetida is not None:
                raise ValueError("Ya existe un líder con esa cédula")
            registro = {
                "id": uuid4().hex,
                "nombre": nombre,
                "cedula": cedula,
                "telefono": telefono,
                "correo": correo,
                "barrio": barrio,
                "candidato_id": candidato_id,
                "candidato": candidato["nombre"],
                "creado_en": datetime.now(ZoneInfo("UTC")).isoformat(),
            }
            columnas = list(registro.keys())
            sql = (
                f"INSERT INTO lideres ({', '.join(columnas)}) "
                f"VALUES ({', '.join(marca for _ in columnas)})"
            )
            conexion.execute(sql, tuple(registro.values()))
        return lider_a_json(registro)


def correo_valido(valor):
    correo = campo(valor, 120).lower()
    if " " in correo or correo.count("@") != 1:
        return ""
    local, dominio = correo.split("@")
    if not local or "." not in dominio or dominio.startswith(".") or dominio.endswith("."):
        return ""
    return correo


def concejal_a_json(fila):
    if not isinstance(fila, dict):
        fila = dict(fila)
    return {
        "id": fila["id"],
        "nombre": fila["nombre"],
        "cedula": fila["cedula"],
        "telefono": fila["telefono"],
        "partido": fila["partido"],
        "tarjeton": fila["tarjeton"],
    }


def lider_a_json(fila):
    if not isinstance(fila, dict):
        fila = dict(fila)
    return {
        "id": fila["id"],
        "nombre": fila["nombre"],
        "cedula": fila["cedula"],
        "telefono": fila["telefono"],
        "correo": fila["correo"],
        "barrio": fila["barrio"],
        "candidatoId": fila["candidato_id"],
        "candidato": fila["candidato"],
    }


def decodificar_audio(valor):
    if valor in (None, ""):
        return None
    if not isinstance(valor, str):
        raise ValueError("El audio no es válido")
    try:
        contenido = base64.b64decode(valor, validate=True)
    except Exception as error:
        raise ValueError("El audio no es válido") from error
    if len(contenido) > 4 * 1024 * 1024:
        raise ValueError("El audio supera el tamaño permitido")
    if len(contenido) < 32:
        return None
    return contenido


CANALES = {"WHATSAPP": "WhatsApp", "LLAMADA": "Llamada"}
CATEGORIAS = {
    "SEGURIDAD": "Seguridad",
    "SERVICIOS": "Servicios públicos",
    "INFRAESTRUCTURA": "Infraestructura",
    "AMBIENTE": "Medio ambiente",
}
COLUMNAS_INFORME = [
    ("Nombre", 28),
    ("Teléfono", 16),
    ("Contacto", 14),
    ("Concejal", 24),
    ("Dirección", 36),
    ("Barrio", 22),
    ("Localidad", 22),
    ("Categoría", 22),
    ("Opinión", 48),
    ("Nota de voz", 14),
    ("Duración (segundos)", 20),
    ("Fecha", 22),
]


def etiqueta_informe(valor, mapa):
    if not valor:
        return ""
    return mapa.get(valor, valor)


def construir_informe(filas, concejales=None, lideres=None):
    libro = Workbook()
    hoja = libro.active
    hoja.title = "Encuestas"
    for columna, (titulo, ancho) in enumerate(COLUMNAS_INFORME, start=1):
        celda = hoja.cell(1, columna, titulo)
        celda.font = Font(bold=True, color="FFFFFF")
        celda.fill = PatternFill("solid", fgColor="0B57D0")
        celda.alignment = Alignment(vertical="center")
        hoja.column_dimensions[get_column_letter(columna)].width = ancho
    for fila_n, item in enumerate(filas, start=2):
        tiene_audio = bool(item.get("tieneAudio"))
        valores = [
            item.get("nombre") or "",
            item.get("telefono") or "",
            etiqueta_informe(item.get("canal"), CANALES),
            item.get("concejal") or "",
            item.get("direccion") or "",
            item.get("barrio") or "",
            item.get("localidad") or "",
            etiqueta_informe(item.get("categoria"), CATEGORIAS),
            item.get("opinion") or "",
            "Sí" if tiene_audio else "No",
            int(item.get("duracionSeg") or 0) if tiene_audio else 0,
            item.get("momento") or "",
        ]
        for columna, valor in enumerate(valores, start=1):
            celda = hoja.cell(fila_n, columna, valor)
            celda.alignment = Alignment(vertical="top", wrap_text=columna == 9)
        hoja.cell(fila_n, 2).number_format = "@"
    ultima = max(1, len(filas) + 1)
    hoja.auto_filter.ref = f"A1:{get_column_letter(len(COLUMNAS_INFORME))}{ultima}"
    hoja.freeze_panes = "A2"
    hoja.page_setup.orientation = "landscape"
    hoja.page_setup.fitToWidth = 1
    hoja.page_setup.fitToHeight = 0
    hoja.sheet_properties.pageSetUpPr.fitToPage = True
    if concejales is not None:
        hoja_concejales = libro.create_sheet("Concejales")
        titulos = [
            ("Nombre", 28),
            ("Cédula", 16),
            ("Teléfono", 16),
            ("Partido", 24),
            ("Número de tarjetón", 20),
        ]
        for columna, (titulo, ancho) in enumerate(titulos, start=1):
            celda = hoja_concejales.cell(1, columna, titulo)
            celda.font = Font(bold=True, color="FFFFFF")
            celda.fill = PatternFill("solid", fgColor="0B57D0")
            hoja_concejales.column_dimensions[get_column_letter(columna)].width = ancho
        for fila_n, item in enumerate(concejales, start=2):
            valores = [
                item.get("nombre") or "",
                item.get("cedula") or "",
                item.get("telefono") or "",
                item.get("partido") or "",
                item.get("tarjeton") or "",
            ]
            for columna, valor in enumerate(valores, start=1):
                celda = hoja_concejales.cell(fila_n, columna, valor)
                if columna in (2, 3, 5):
                    celda.number_format = "@"
        ultima_concejal = max(1, len(concejales) + 1)
        hoja_concejales.auto_filter.ref = f"A1:E{ultima_concejal}"
        hoja_concejales.freeze_panes = "A2"
    if lideres is not None:
        hoja_lideres = libro.create_sheet("Lideres")
        titulos_lider = [
            ("Nombre completo", 32),
            ("Cédula", 16),
            ("Teléfono", 16),
            ("Correo", 32),
            ("Barrio", 24),
            ("Líder del candidato", 28),
        ]
        for columna, (titulo, ancho) in enumerate(titulos_lider, start=1):
            celda = hoja_lideres.cell(1, columna, titulo)
            celda.font = Font(bold=True, color="FFFFFF")
            celda.fill = PatternFill("solid", fgColor="0B57D0")
            hoja_lideres.column_dimensions[get_column_letter(columna)].width = ancho
        for fila_n, item in enumerate(lideres, start=2):
            valores = [
                item.get("nombre") or "",
                item.get("cedula") or "",
                item.get("telefono") or "",
                item.get("correo") or "",
                item.get("barrio") or "",
                item.get("candidato") or "",
            ]
            for columna, valor in enumerate(valores, start=1):
                celda = hoja_lideres.cell(fila_n, columna, valor)
                if columna in (2, 3):
                    celda.number_format = "@"
        ultima_lider = max(1, len(lideres) + 1)
        hoja_lideres.auto_filter.ref = f"A1:F{ultima_lider}"
        hoja_lideres.freeze_panes = "A2"
    buffer = BytesIO()
    libro.save(buffer)
    return buffer.getvalue()


def fila_a_json(fila):
    if not isinstance(fila, dict):
        fila = dict(fila)
    return {
        "id": fila["id"],
        "nombre": fila["nombre"],
        "telefono": fila["telefono"],
        "barrio": fila.get("barrio") or "",
        "direccion": fila.get("direccion") or "",
        "localidad": fila.get("localidad") or "",
        "canal": fila.get("canal") or "",
        "categoria": fila.get("categoria") or "",
        "concejal": fila.get("concejal") or "",
        "opinion": fila["opinion"],
        "momento": fila["momento"],
        "tieneAudio": bool(fila["tiene_audio"]),
        "duracionSeg": int(fila["duracion_seg"]),
        "estado": fila["estado"],
        "tono": fila["tono"],
        "acento": fila["acento"],
        "urgente": bool(fila["urgente"]),
    }


def iniciar_almacen():
    ultimo_error = None
    for _ in range(10):
        try:
            almacen.preparar()
            return
        except Exception as error:
            ultimo_error = error
            time.sleep(3)
    raise RuntimeError("No se pudo abrir la base de respuestas") from ultimo_error


almacen = Almacen()
iniciar_almacen()


@app.get("/")
def inicio():
    return pagina("formulario.html")


@app.get("/panel")
def panel():
    if not es_admin():
        return redirect("/?entrar=1")
    return pagina("panel.html")


@app.get("/api/sesion")
def ver_sesion():
    return jsonify(admin=es_admin())


@app.post("/api/sesion")
def entrar():
    if not CLAVE_PANEL:
        return jsonify(error="La clave del panel no está configurada"), 503
    datos = request.get_json(silent=True) or {}
    clave = datos.get("clave")
    if not isinstance(clave, str):
        clave = ""
    if not hmac.compare_digest(clave.encode("utf-8"), CLAVE_PANEL.encode("utf-8")):
        return jsonify(error="Clave incorrecta"), 401
    session.clear()
    session["admin"] = True
    session.permanent = True
    return jsonify(ok=True)


@app.delete("/api/sesion")
def salir():
    session.clear()
    return jsonify(ok=True)


@app.get("/api/respuestas")
def listar_respuestas():
    return jsonify(almacen.listar())


@app.post("/api/respuestas")
def crear_respuesta():
    datos = request.get_json(silent=True)
    if not isinstance(datos, dict):
        return jsonify(error="El envío tiene que ser JSON"), 400
    try:
        creada = almacen.guardar(datos)
    except ValueError as error:
        return jsonify(error=str(error)), 400
    return jsonify(creada), 201


@app.delete("/api/respuestas")
def borrar_respuestas():
    if not es_admin():
        return jsonify(error="Inicia sesión para eliminar"), 401
    datos = request.get_json(silent=True) or {}
    identificadores = datos.get("ids")
    if not isinstance(identificadores, list) or not identificadores:
        return jsonify(error="Selecciona encuestas para eliminar"), 400
    return jsonify(eliminadas=almacen.eliminar(identificadores))


@app.get("/api/informe.xlsx")
def descargar_informe():
    if not es_admin():
        return jsonify(error="Inicia sesión para descargar el informe"), 401
    contenido = construir_informe(almacen.listar(), almacen.listar_concejales(), almacen.listar_lideres())
    fecha = datetime.now(ZoneInfo("America/Bogota")).strftime("%Y-%m-%d")
    nombre = f"vozcomunidad-{fecha}.xlsx"
    return Response(
        contenido,
        mimetype="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": f'attachment; filename="{nombre}"'},
    )


@app.get("/api/concejales")
def listar_concejales():
    lista = almacen.listar_concejales()
    if es_admin():
        return jsonify(lista)
    return jsonify([{"id": item["id"], "nombre": item["nombre"], "partido": item["partido"], "tarjeton": item["tarjeton"]} for item in lista])


@app.post("/api/concejales")
def crear_concejal():
    if not es_admin():
        return jsonify(error="Inicia sesión para crear concejales"), 401
    datos = request.get_json(silent=True)
    if not isinstance(datos, dict):
        return jsonify(error="El envío tiene que ser JSON"), 400
    try:
        creado = almacen.guardar_concejal(datos)
    except ValueError as error:
        return jsonify(error=str(error)), 400
    return jsonify(creado), 201


@app.get("/api/lideres")
def listar_lideres():
    if not es_admin():
        return jsonify(error="Inicia sesión para ver los líderes"), 401
    return jsonify(almacen.listar_lideres())


@app.post("/api/lideres")
def crear_lider():
    if not es_admin():
        return jsonify(error="Inicia sesión para crear líderes"), 401
    datos = request.get_json(silent=True)
    if not isinstance(datos, dict):
        return jsonify(error="El envío tiene que ser JSON"), 400
    try:
        creado = almacen.guardar_lider(datos)
    except ValueError as error:
        return jsonify(error=str(error)), 400
    return jsonify(creado), 201


@app.get("/salud")
def salud():
    return jsonify(audio=True, version=11)


def tipo_audio(contenido):
    if contenido.startswith(b"RIFF"):
        return "audio/wav", "nota-de-voz.wav"
    if contenido.startswith(b"\x1aE\xdf\xa3"):
        return "audio/webm", "nota-de-voz.webm"
    return "audio/mp4", "nota-de-voz.m4a"


def pagina(nombre):
    texto = Path(__file__).with_name(nombre).read_text(encoding="utf-8")
    return Response(texto, mimetype="text/html; charset=utf-8", headers={"Cache-Control": "no-store"})


def es_admin():
    return bool(session.get("admin"))


@app.get("/api/respuestas/<identificador>/audio")
def servir_audio(identificador):
    contenido = almacen.leer_audio(identificador)
    if not contenido:
        return jsonify(error="Esta respuesta no tiene audio"), 404
    mime, nombre = tipo_audio(contenido)
    return Response(
        contenido,
        mimetype=mime,
        headers={
            "Content-Length": str(len(contenido)),
            "Accept-Ranges": "bytes",
            "Content-Disposition": "inline; filename=" + nombre,
        },
    )
