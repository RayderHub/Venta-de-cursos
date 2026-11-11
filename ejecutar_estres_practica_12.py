import urllib.request
import urllib.error
import time
import json
import statistics
import concurrent.futures

RENDER_URL = "https://skillacademy-backend-at8o.onrender.com"

def benchmark_endpoint(url, method="GET", payload=None, concurrency=10, total_requests=50):
    latencies = []
    status_codes = {}
    errors = 0
    total_bytes = 0

    headers = {
        "User-Agent": "SkillAcademy-StressTester/1.0",
        "Content-Type": "application/json"
    }
    encoded_payload = json.dumps(payload).encode("utf-8") if payload else None

    def worker():
        t0 = time.perf_counter()
        try:
            req = urllib.request.Request(url, data=encoded_payload, headers=headers, method=method)
            with urllib.request.urlopen(req, timeout=12) as resp:
                data = resp.read()
                elapsed = (time.perf_counter() - t0) * 1000
                return {"code": resp.status, "ms": elapsed, "bytes": len(data), "ok": True}
        except urllib.error.HTTPError as e:
            elapsed = (time.perf_counter() - t0) * 1000
            return {"code": e.code, "ms": elapsed, "bytes": 0, "ok": False}
        except Exception as e:
            elapsed = (time.perf_counter() - t0) * 1000
            return {"code": 500, "ms": elapsed, "bytes": 0, "ok": False}

    start_wall = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(worker) for _ in range(total_requests)]
        for f in concurrent.futures.as_completed(futures):
            res = f.result()
            latencies.append(res["ms"])
            code = res["code"]
            status_codes[code] = status_codes.get(code, 0) + 1
            total_bytes += res["bytes"]
            if not res["ok"]:
                errors += 1

    total_wall_time = time.perf_counter() - start_wall
    latencies.sort()
    p90_idx = int(0.90 * len(latencies))
    p95_idx = int(0.95 * len(latencies))

    rps = total_requests / total_wall_time if total_wall_time > 0 else 0
    throughput_kbps = (total_bytes * 8) / (total_wall_time * 1000) if total_wall_time > 0 else 0

    return {
        "url": url,
        "method": method,
        "concurrency": concurrency,
        "total": total_requests,
        "wall_time_s": round(total_wall_time, 3),
        "rps": round(rps, 2),
        "avg_ms": round(statistics.mean(latencies), 2),
        "min_ms": round(min(latencies), 2),
        "max_ms": round(max(latencies), 2),
        "p90_ms": round(latencies[min(p90_idx, len(latencies)-1)], 2),
        "p95_ms": round(latencies[min(p95_idx, len(latencies)-1)], 2),
        "errors": errors,
        "error_pct": round((errors / total_requests) * 100, 2),
        "status_codes": status_codes,
        "throughput_kbps": round(throughput_kbps, 2)
    }

print("=== 1. Prueba de Estrés: Endpoint de Lectura (Catálogo Search) ===")
res_read = benchmark_endpoint(f"{RENDER_URL}/api/cursos/search?q=excel", method="GET", concurrency=10, total_requests=30)
print(json.dumps(res_read, indent=2))

print("\n=== 2. Prueba de Estrés: Endpoint de Escritura (Actualización de Widget) ===")
payload_widget = {
    "nombre": "SkillAcademy Wear Stress",
    "dispositivo": "Wear OS QA",
    "titulo": "Prueba de carga y concurrencia",
    "mensaje": "Evaluacion de mutaciones simultaneas",
    "curso": "Excel Avanzado",
    "progreso": 95
}
res_write = benchmark_endpoint(f"{RENDER_URL}/api/widget/wearable", method="PUT", payload=payload_widget, concurrency=10, total_requests=30)
print(json.dumps(res_write, indent=2))

with open("estres_antes_optimizacion.json", "w", encoding="utf-8") as f:
    json.dump({"lectura": res_read, "escritura": res_write}, f, indent=2)

print("\n=== Benchmarks iniciales completados ===")
