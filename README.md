# 🚦 TraaShTrafficSimulator

A sophisticated real-time traffic simulation visualizer built with JavaFX and SUMO, featuring advanced coordinate transformation, interactive navigation, and object-oriented architecture designed for extensibility and performance.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-17+-orange.svg)
![JavaFX](https://img.shields.io/badge/JavaFX-21+-green.svg)
![SUMO](https://img.shields.io/badge/SUMO-1.15+-red.svg)

## 🌟 Features

### 🎯 Core Visualization
- **Real-time Traffic Display**: Live visualization of SUMO simulation data at 60 FPS
- **Multi-lane Road Rendering**: Accurate bidirectional roads with proper lane markings
- **Vehicle Type Distinction**: Visual differentiation between cars, trucks, buses, motorcycles
- **Professional UI**: Dark theme with intuitive controls

### 🔧 Interactive Navigation
- **Smooth Pan & Zoom**: Mouse-driven navigation with momentum-based feel
- **Zoom-to-Point**: Intelligent zoom that maintains cursor position
- **Reset View**: One-click return to optimal network view
- **Click Detection**: Interactive selection of lanes and vehicles

### 🏗️ Technical Excellence
- **Object-Oriented Architecture**: Clean separation of concerns with visual objects
- **Coordinate Transformation System**: Sophisticated world-to-screen conversion
- **Performance Optimized**: Efficient rendering with culling and caching
- **Extensible Design**: Easy addition of new features and vehicle types

## 🔬 Technical Architecture

### 📐 Coordinate System Mathematics

The application handles complex coordinate transformations between different systems:

#### **1. SUMO World Coordinates**
```
Origin: Bottom-left (0,0)
X-Axis: Increases rightward
Y-Axis: Increases upward
Units: Meters
```

#### **2. JavaFX Screen Coordinates**
```
Origin: Top-left (0,0)
X-Axis: Increases rightward  
Y-Axis: Increases downward
Units: Pixels
```

#### **3. Transformation Pipeline**
```mathematica
# World to Screen Transformation
screenX = (worldX * baseScale * zoom) + baseOffset.x + userPan.x
screenY = canvasHeight - ((worldY * baseScale * zoom) + baseOffset.y + userPan.y)

# Screen to World Transformation  
worldX = (screenX - baseOffset.x - userPan.x) / (baseScale * zoom)
worldY = ((canvasHeight - screenY) - baseOffset.y - userPan.y) / (baseScale * zoom)
```

### 🚗 Vehicle Angle Conversion

SUMO and JavaFX use different angle conventions, requiring mathematical conversion:

```java
// SUMO: 0° = North, 90° = East, clockwise
// JavaFX: 0° = East, 90° = South, clockwise  
// Y-axis flip affects rotation direction

double sumoAngle = vehicleData[2];           // From SUMO
double javaFXAngle = sumoAngle - 90;   // Convert for JavaFX
```

**Angle Mapping Examples:**
- SUMO North (0°) → JavaFX (-90°) = North
- SUMO East (90°) → JavaFX (0°) = East  
- SUMO South (180°) → JavaFX (90°) = South
- SUMO West (270°) → JavaFX (180°) = West

## 🚀 Installation & Setup

### 📋 Prerequisites

| Component | Version | Purpose |
|-----------|---------|---------|
| **Java** | 17+ | Runtime environment |
| **JavaFX** | 21+ | GUI framework |
| **SUMO** | 1.15+ | Traffic simulation |
| **OS** | Linux/macOS | Development platform |

### 🛠️ Installation Steps

1. **Clone Repository**
   ```bash
   git clone https://github.com/herrcult69/TraaShTrafficSimulator.git
   cd TraaShTrafficSimulator
   ```

2. **Install SUMO**
   ```bash
   # Ubuntu/Debian
   sudo apt update
   sudo apt install sumo sumo-tools sumo-doc
   
   # Verify installation
   sumo --version
   ```

3. **Setup JavaFX**
   ```bash
   # Download JavaFX SDK from https://openjfx.io/
   # Extract to lib/javafx/ directory
   ```

4. **Verify Project Structure**
   ```
   TraaShTrafficSimulator/
   ├── src/                      # Java source files
   │   ├── TrafficSimulatorApp.java
   │   ├── NetworkParser.java
   │   ├── SimulationRunner.java
   │   ├── TrafficScene.java
   │   ├── CoordinateTransform.java
   │   ├── VisualEdge.java
   │   ├── VisualLane.java
   │   └── VisualVehicle.java
   ├── lib/
   │   ├── javafx/              # JavaFX libraries
   │   └── TraaS.jar           # SUMO-Java integration
   ├── resource/               # SUMO configuration
   │   ├── network.net.xml     # Road network
   │   └── simulation.sumocfg  # Simulation settings
   └── bin/                    # Compiled classes
   ```

## 🎮 Usage

### 🔨 Compilation
```bash
# Compile all source files with proper classpath
javac -cp ".:lib/*:lib/javafx/*" -d bin src/*.java
```

### ▶️ Execution
```bash
# Launch with full JavaFX module path
java -cp "bin:lib/javafx/*:lib/TraaS.jar" \
     --module-path lib/javafx \
     --add-modules javafx.controls,javafx.fxml \
     TrafficSimulatorApp
```

### 🎛️ Controls

| Action | Input | Description |
|--------|-------|-------------|
| **Pan** | Mouse Drag | Move view around network |
| **Zoom In** | Mouse Wheel Up | Zoom toward cursor position |
| **Zoom Out** | Mouse Wheel Down | Zoom away from cursor position |
| **Zoom In** | + Button | Zoom 20% toward center |
| **Zoom Out** | - Button | Zoom 20% away from center |
| **Reset View** | Reset Button | Fit entire network optimally |
| **Select Object** | Mouse Click | Identify vehicles/lanes (console output) |

## 🏗️ Architecture Deep Dive

### 📊 Class Hierarchy

```
TrafficSimulatorApp (JavaFX Application)
├── NetworkParser (XML Processing)
├── SimulationRunner (SUMO Integration)
├── TrafficScene (Scene Graph Manager)
├── CoordinateTransform (Math Operations)
└── Visual Objects (Renderable Components)
    ├── VisualEdge (Road Rendering)
    ├── VisualLane (Lane Hit Detection)
    └── VisualVehicle (Vehicle Animation)
```

### 🎨 Key Algorithms

#### **1. Network Fitting Algorithm**
```java
// Calculate optimal scale to fit network with margins
scale = min((canvasWidth - 2*margin) / networkWidth, 
           (canvasHeight - 2*margin) / networkHeight);

// Center network on canvas
offsetX = (canvasWidth - networkWidth*scale)/2 - networkMinX*scale;
offsetY = (canvasHeight - networkHeight*scale)/2 - networkMinY*scale;
```

#### **2. Zoom-to-Point Transformation**
```java
// Convert mouse position to world coordinates
worldX = (mouseX - offsetX - panX) / (scale * zoom);
worldY = ((canvasHeight - mouseY) - offsetY - panY) / (scale * zoom);

// Apply zoom and adjust pan to keep point stationary
zoom *= factor;
panX += mouseX - ((worldX * scale * zoom) + offsetX + panX);
panY += mouseY - (canvasHeight - ((worldY * scale * zoom) + offsetY + panY));
```

#### **3. Vehicle Angle Compensation**
```java
// SUMO uses Y-up coordinate system, JavaFX uses Y-down
// Vehicle angles need Y-axis flip compensation to appear correct
double displayAngle = -sumoAngle;  // Negate to flip Y-axis

// Rotate vehicle rendering context for proper orientation
gc.save();
gc.translate(screenX, screenY);
gc.rotate(Math.toDegrees(displayAngle));
gc.fillRect(-width/2, -height/2, width, height);  // Draw centered
gc.restore();
```

#### **4. Lane Spacing Distribution**
```java
// Distribute lanes evenly across edge width with proper spacing
double laneWidth = 3.2;  // Standard SUMO lane width in meters
double totalWidth = edge.getLanes().size() * laneWidth;

for (int i = 0; i < numLanes; i++) {
    // Calculate lane center offset from edge centerline
    double laneOffset = (i - (numLanes - 1) / 2.0) * laneWidth;
    
    // Apply perpendicular offset to edge direction vector
    double laneX = edgeCenterX + laneOffset * perpendicularX;
    double laneY = edgeCenterY + laneOffset * perpendicularY;
}
```

## 🔧 Configuration Files

### 🗺️ network.net.xml
**Purpose**: Defines the complete road network topology including junctions, edges, lanes, and traffic rules.

**Key Components**:
- **Junctions**: Intersection nodes with coordinates and traffic logic
- **Edges**: Road segments connecting junctions with geometry and lane specifications
- **Lanes**: Individual traffic lanes with speed limits, length, and shape definitions
- **Connections**: Define allowed turns and traffic flow between lanes

**Mathematical Properties**:
- Coordinates use SUMO's metric system (meters)
- Y-axis points upward (mathematical convention)
- Junction positions define network bounds for auto-fitting
- Lane shapes defined as sequences of (x,y) coordinate points

### 🚗 Route Files (*.rou.xml)
**Purpose**: Define vehicle types, spawning patterns, and route assignments.

**File Structure**:
- `cars.rou.xml`: Red passenger cars, spawn rate 1/200s
- `trucks.rou.xml`: Blue freight vehicles, spawn rate 1/300s  
- `motorcycles.rou.xml`: Black bikes, spawn rate 1/150s
- `buses.rou.xml`: Green public transit, spawn rate 1/400s
- `emergency.rou.xml`: Yellow ambulances/fire trucks, spawn rate 1/500s

**Vehicle Properties**:
```xml
<vType id="car" accel="2.6" decel="4.5" sigma="0.5" 
       length="5" maxSpeed="55.56" color="red"/>
```

### ⚙️ simulation.sumocfg
**Purpose**: Master configuration orchestrating all simulation components.

**Critical Settings**:
- **Time Window**: `<time begin="0" end="3600"/>` (1 hour simulation)
- **Step Length**: `<step-length value="0.1"/>` (100ms precision)
- **Network**: References `network.net.xml` for topology
- **Routes**: Includes all vehicle type and route definitions
- **Output**: Configures data export and logging options

## 🚀 Build & Run Instructions

### 📋 Prerequisites
```bash
# Java Development Kit 17+
java --version

# SUMO Traffic Simulator
sudo apt install sumo sumo-tools sumo-doc
echo 'export SUMO_HOME="/usr/share/sumo"' >> ~/.bashrc

# JavaFX SDK (if not bundled with JDK)
# Download from https://openjfx.io/ and extract to lib/javafx/
```

### 🔨 Compilation
```bash
cd TraaShTrafficSimulator

# Compile with full classpath
javac -cp ".:lib/*:lib/javafx/*" -d bin src/*.java

# Alternative: Compile individual components
javac -cp "lib/TraaS.jar" -d bin src/{NetworkParser,SimulationRunner,TraaSAdapter}.java
javac -cp "bin:lib/TraaS.jar:lib/javafx/*" -d bin src/{CoordinateTransform,TrafficScene}.java
javac -cp "bin:lib/TraaS.jar:lib/javafx/*" -d bin src/{VisualEdge,VisualLane,VisualVehicle}.java
javac -cp "bin:lib/TraaS.jar:lib/javafx/*" -d bin src/TrafficSimulatorApp.java
```

### ▶️ Execution
```bash
# Launch JavaFX application with full module support
java -cp "bin:lib/javafx/*:lib/TraaS.jar" \
     --module-path lib/javafx \
     --add-modules javafx.controls,javafx.fxml \
     TrafficSimulatorApp

# Alternative: Direct Java execution (if JavaFX bundled)
java -cp "bin:lib/TraaS.jar" TrafficSimulatorApp
```

## 🛠️ Troubleshooting

### 🔍 Common Issues

**❌ ClassNotFoundException: TrafficSimulatorApp**
```bash
# Ensure proper compilation and classpath
ls -la bin/  # Check compiled classes exist
java -cp "bin:lib/TraaS.jar" -XshowSettings:class TrafficSimulatorApp
```

**❌ JavaFX Runtime Components Missing**
```bash
# Install OpenJFX
sudo apt install openjfx

# Or download and configure JavaFX SDK
export JAVAFX_HOME="/path/to/javafx-sdk"
export PATH="$JAVAFX_HOME/bin:$PATH"
```

**❌ SUMO Connection Failed**
```bash
# Verify SUMO installation
which sumo
sumo --version

# Check TraCI port availability
netstat -ln | grep 8813
```

**❌ Network Parsing Errors**
```bash
# Validate SUMO network file
sumo-gui -n resource/network.net.xml --quit-on-end

# Check XML syntax
xmllint --noout resource/network.net.xml
```

### 📊 Performance Optimization

**Memory Management**:
- Increase heap size: `-Xmx2g -Xms512m`
- Optimize GC: `-XX:+UseG1GC -XX:MaxGCPauseMillis=100`

**Rendering Performance**:
- Reduce update frequency: `Timeline.setRate(0.5)` for 2Hz updates
- Enable hardware acceleration: `-Dprism.order=hw`
- Limit visible vehicle count: Filter based on screen bounds

## 🤝 Contributing

### 🏗️ Development Setup

1. **Fork and Clone**
   ```bash
   git clone https://github.com/yourusername/TraaShTrafficSimulator.git
   cd TraaShTrafficSimulator
   ```

2. **IDE Configuration**
   - **VS Code**: Install "Extension Pack for Java" and "JavaFX Support"
   - **IntelliJ**: Enable JavaFX plugin and configure module path
   - **Eclipse**: Install e(fx)clipse plugin for JavaFX support

3. **Code Style**
   - Follow Java naming conventions (camelCase, PascalCase)
   - Document all public methods with JavaDoc
   - Include mathematical formulas in comments where applicable
   - Use meaningful variable names describing coordinate systems

### 🎯 Architecture Guidelines

**Adding New Visual Objects**:
1. Extend base functionality with `contains()` and `render()` methods
2. Implement coordinate transformation using `CoordinateTransform`
3. Handle both SUMO world coordinates and JavaFX screen coordinates
4. Include proper Y-axis flip compensation for angles

**Performance Considerations**:
- Cache transformed coordinates when possible
- Use efficient hit-testing algorithms (bounding boxes before detailed checks)
- Batch rendering operations to minimize JavaFX calls
- Profile memory usage during long-running simulations

## 📈 Future Enhancements

### 🎨 Visualization Features
- **Traffic Light States**: Render junction signals with realistic timing
- **Lane Markings**: Detailed road striping and signage
- **Vehicle Details**: Speed indicators, turn signals, brake lights
- **Network Analysis**: Heat maps for traffic density and flow patterns

### 🔧 Technical Improvements
- **Multi-Threading**: Separate rendering and simulation threads
- **Config Management**: GUI-based parameter adjustment
- **Data Export**: CSV/JSON export for analysis tools
- **Plugin Architecture**: Extensible visualization components

### 🚀 Advanced Algorithms
- **Predictive Rendering**: Pre-calculate vehicle paths for smooth animation
- **Spatial Indexing**: Quadtree/octree for efficient object queries
- **Dynamic LOD**: Level-of-detail based on zoom level
- **Smart Culling**: Only render objects within viewport

## 📚 References & Documentation

### 📖 Technical Resources
- **[SUMO Documentation](https://sumo.dlr.de/docs/)**: Comprehensive traffic simulation guide
- **[TraaS JavaDoc](https://sumo.dlr.de/javadoc/traas/)**: Java API for SUMO integration
- **[JavaFX Documentation](https://openjfx.io/javadoc/17/)**: GUI framework reference
- **[SUMO Network Format](https://sumo.dlr.de/docs/Networks/SUMO_Road_Networks.html)**: XML schema documentation

### 🧮 Mathematical Background  
- **Coordinate Transformations**: Linear algebra for 2D projections and rotations
- **Affine Transformations**: Pan, zoom, and scale operations in computer graphics
- **Traffic Flow Theory**: Fundamental diagrams and microscopic simulation principles
- **Spatial Algorithms**: Computational geometry for intersection detection

---

**License**: MIT  
**Version**: 2.0.0  
**Author**: Traffic Simulation Team  
**Last Updated**: 2024

*For questions, issues, or contributions, please visit our [GitHub repository](https://github.com/yourusername/TraaShTrafficSimulator).*
