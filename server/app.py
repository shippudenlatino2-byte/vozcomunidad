import os
import sqlite3
import time
import base64
from contextlib import contextmanager
from datetime import datetime
from uuid import uuid4
from zoneinfo import ZoneInfo

from flask import Flask, Response, jsonify, request
from pathlib import Path

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 6 * 1024 * 1024

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

    def asegurar_audio(self, conexion):
        if "audio" in self.columnas(conexion):
            return
        tipo = "BYTEA" if self.postgres else "BLOB"
        conexion.execute(f"ALTER TABLE respuestas ADD COLUMN audio {tipo}")

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
            SELECT id, nombre, telefono, barrio, opinion, momento, creado_en,
                   tiene_audio, duracion_seg, estado, tono, acento, urgente
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
            "barrio": campo(datos.get("barrio"), 80) or "Sin barrio",
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
        "barrio": fila["barrio"],
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
    pagina = Path(__file__).with_name("panel.html").read_text(encoding="utf-8")
    return Response(pagina, mimetype="text/html; charset=utf-8")


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


@app.get("/salud")
def salud():
    return jsonify(audio=True, version=3)


@app.get("/api/respuestas/<identificador>/audio")
def servir_audio(identificador):
    contenido = almacen.leer_audio(identificador)
    if not contenido:
        return jsonify(error="Esta respuesta no tiene audio"), 404
    return Response(contenido, mimetype="audio/mp4")
