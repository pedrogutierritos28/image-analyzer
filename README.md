# 🖼️ Image Analyzer

Aplicación de escritorio en Java para visualizar y procesar imágenes con una interfaz gráfica moderna. Permite aplicar filtros, ajustar canales de color y analizar el histograma RGB en tiempo real.

---

## ✨ Características

- **Carga de imágenes** por diálogo de archivo o arrastrar y soltar (drag & drop)
- **Histograma RGB + Luminosidad** con escala lineal o logarítmica y visibilidad por canal
- **Estadísticas por canal** (media, desviación estándar y mediana para R, G, B)
- **Filtros de imagen:**
  - Blanco y negro (luminancia perceptual ITU-R BT.709)
  - Negativo (inversión de color)
  - Binarización con umbral ajustable (0–255)
- **Ajuste de canales de color** (offset R, G, B de −255 a +255)
- **Guardado de imagen** en PNG, JPEG o BMP con nombre sugerido automáticamente
- **Limpieza** del espacio de trabajo con un clic

---

## 🛠️ Requisitos

- Java 21 o superior
- Maven 3.6+

---

## 🚀 Instalación y ejecución

### Clonar el repositorio

```bash
git clone https://github.com/pedrogutierritos28/image-analyzer.git
cd image-analyzer
```

### Ejecutar con Maven

```bash
mvn exec:java
```

### Generar JAR ejecutable

```bash
mvn package
java -jar target/AnalyzerImage.jar
```
