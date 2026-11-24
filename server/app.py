import os
import hmac
import hashlib
import sqlite3
import time
import base64
from contextlib import contextmanager
from datetime import datetime, timedelta
from uuid import uuid4
from zoneinfo import ZoneInfo

from flask import Flask, Response, jsonify, redirect, request, session
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
            for nombre in ("canal", "direccion", "localidad", "categoria"):
                self.asegurar_texto(conexion, nombre)

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
            SELECT id, nombre, telefono, barrio, direccion, localidad, canal, categoria,
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


@app.get("/salud")
def salud():
    return jsonify(audio=True, version=6)


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
