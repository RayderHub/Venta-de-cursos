# -*- coding: utf-8 -*-
"""Script rápido para inspeccionar el contenido del docx y ubicar secciones de pantallas."""
import os
import docx

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCX = os.path.join(BASE, "entregables", "SkillAcademy_Mobile_Native_Entregables.docx")

doc = docx.Document(DOCX)
print(f"Total párrafos: {len(doc.paragraphs)}")
print(f"Total tablas: {len(doc.tables)}\n")

# Recorrer párrafos e imprimir los que parecen encabezados de pantallas o secciones
for i, p in enumerate(doc.paragraphs):
    t = p.text.strip()
    if not t:
        continue
    # Headings o texto interesante
    style = p.style.name if p.style else ""
    keywords = [
        "Pantalla", "Home", "Catálogo", "Catalog", "Detalle", "Curso", "Carrito", "Login",
        "Registro", "Checkout", "Aula", "Mis Cursos", "Perfil", "ConsultaWeb",
        "Figura", "EVIDENCIA", "CAPTURA", "Ficha", "Pasarela", "Pago",
        "Entregable 1", "Entregable 2", "Entregable 3",
        "HomeScreen", "CatalogScreen", "CourseDetail", "CartScreen", "LoginScreen",
        "RegisterScreen", "CourseLearn", "MyCourses", "ProfileScreen",
        "Scrolling", "Navegación", "Acomodado", "Estándar Americano"
    ]
    if any(k.lower() in t.lower() for k in keywords) or "Heading" in style:
        print(f"[{i:04d}] ({style:20s}) {t[:160]}")
