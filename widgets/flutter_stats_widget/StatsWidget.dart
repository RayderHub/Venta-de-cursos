import 'package:flutter/material.dart';

class StatsWidget extends StatelessWidget {
  const StatsWidget({
    super.key,
    required this.totalTareas,
    required this.pendientes,
    required this.completadas,
    required this.progresoPromedio,
  });

  final int totalTareas;
  final int pendientes;
  final int completadas;
  final int progresoPromedio;

  @override
  Widget build(BuildContext context) {
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 12,
      mainAxisSpacing: 12,
      children: [
        _StatCard(label: 'Progreso', value: '$progresoPromedio%'),
        _StatCard(label: 'Tareas', value: '$totalTareas'),
        _StatCard(label: 'Pendientes', value: '$pendientes'),
        _StatCard(label: 'Completadas', value: '$completadas'),
      ],
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(22),
        boxShadow: const [
          BoxShadow(
            color: Color(0x1F111A15),
            blurRadius: 26,
            offset: Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            label,
            style: const TextStyle(
              color: Color(0xFF617066),
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            value,
            style: const TextStyle(
              color: Color(0xFF17211B),
              fontSize: 30,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }
}
