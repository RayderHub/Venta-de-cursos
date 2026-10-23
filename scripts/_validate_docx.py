# -*- coding: utf-8 -*-
"""Validación: imprimir los specs de las primeras 6 tablas de captura procesadas."""
import os
import docx

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCX = os.path.join(BASE, "entregables", "SkillAcademy_Mobile_Native_Entregables.docx")

doc = docx.Document(DOCX)
print(f"Total tablas: {len(doc.tables)}\n")

contados = 0
for ti, tbl in enumerate(doc.tables):
    if len(tbl.rows) != 1 or len(tbl.columns) != 1:
        continue
    cell = tbl.rows[0].cells[0]
    texto = cell.text
    if not any(x in texto for x in ["📷", "📸", "CAPTURA", "EVIDENCIA"]):
        continue

    # Imprimir las líneas que contienen los nuevos campos
    lineas = texto.splitlines()
    titulo = next((l for l in lineas if any(x in l for x in ["📷", "📸"])), "Sin título")

    tiene_acomodado = any("📐" in l or "Acomodado" in l for l in lineas)
    tiene_navegacion_cell = any("🧭" in l or "Tipo de Navegación" in l for l in lineas)
    tiene_pie_img = any("Navegación en pantalla" in l for l in lineas)
    contados += 1

    # Extraer las 2 líneas specs y el pie
    print(f"--- Bloque #{contados}: {titulo[:110]} ---")
    print(f"  📐 Acomodado presente:            {str(tiene_acomodado).upper()}")
    print(f"  🧭 Tipo Navegación spec presente: {str(tiene_navegacion_cell).upper()}")
    print(f"  🧭 Pie de foto navegación:        {str(tiene_pie_img).upper()}")

    # Imprimir el texto de los nuevos campos (primeras 180 chars)
    for l in lineas:
        if any(x in l for x in ["📐 Tipo de Acomodado", "🧭 Tipo de Navegación", "🧭 Navegación en pantalla"]):
            print(f"   → {l[:200]}")
    print()
    if contados >= 15:
        break

print(f"\n✅ Total bloques validados: {contados}")
