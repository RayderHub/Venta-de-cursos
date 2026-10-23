# -*- coding: utf-8 -*-
"""Inspeccionar las tablas (bloques de captura de pantalla) para ver su estructura."""
import os
import docx

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCX = os.path.join(BASE, "entregables", "SkillAcademy_Mobile_Native_Entregables.docx")

doc = docx.Document(DOCX)
print(f"Total tablas: {len(doc.tables)}\n")

for ti, tbl in enumerate(doc.tables):
    # Buscar el párrafo anterior (título de la sección)
    # Imprimir solo tablas que parecen ser de captura (1 columna, texto con 'Pantalla' o '📷' o 'EVIDENCIA' o '📌')
    rows = len(tbl.rows)
    cols = len(tbl.columns)
    # Sacar los primeros 300 chars del contenido de la primera celda
    first_cell_text = ""
    try:
        first_cell_text = tbl.rows[0].cells[0].text.strip().replace("\n", " | ")[:300]
    except Exception:
        pass

    # Contar imágenes en la tabla
    img_count = 0
    try:
        from docx.oxml.ns import qn
        for row in tbl.rows:
            for cell in row.cells:
                for p in cell.paragraphs:
                    for run in p.runs:
                        if run._element.findall(qn("w:drawing")) or run._element.findall(qn("w:pict")):
                            img_count += 1
    except Exception:
        pass

    # Filtrar tablas interesantes
    interesting = any(k in first_cell_text.upper() for k in [
        "PANTALLA", "EVIDENCIA", "CAPTURA", "📷", "HOME", "CATALOGO", "CATÁLOGO",
        "CURSO", "CARRITO", "LOGIN", "REGISTRO", "CUPÓN", "PAGO", "AULA", "PERFIL",
        "CONSULTA", "CHECKOUT"
    ])

    if interesting or img_count > 0:
        print(f"=== Tabla #{ti} | rows={rows} cols={cols} | imgs={img_count} ===")
        print(f"  Primer celda: {first_cell_text}")
        print()
