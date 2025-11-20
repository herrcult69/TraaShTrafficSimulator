# TraaShTrafficSimulator

A real-time traffic simulation visualizer built with JavaFX and SUMO (Simulation of Urban Mobility). This application provides an interactive interface for viewing and analyzing traffic flow patterns with object-oriented architecture designed for extensibility.

## 🚀 Features

### Core Functionality
- **Real-time Visualization**: Live display of SUMO traffic simulation data
- **Interactive Navigation**: Pan, zoom, and reset view controls with smooth transitions
- **Multi-lane Roads**: Accurate representation of bidirectional roads with proper lane markings
- **Vehicle Types**: Visual distinction between cars, trucks, buses, and motorcycles
- **Click Detection**: Interactive selection of lanes and vehicles

### Technical Highlights
- **Object-Oriented Architecture**: Clean separation between data, visual objects, and UI
- **Scalable Rendering**: Coordinate transformation system with proper scaling at all zoom levels
- **Performance Optimized**: Efficient rendering with culling and minimum size constraints
- **Extensible Design**: Easy to add new vehicle types, road elements, and interaction features

## 🏗️ Architecture

### Class Structure

```
TrafficSimulatorApp (Main Application)
├── NetworkParser (SUMO XML Parsing)
├── SimulationRunner (SUMO Integration)  
├── TrafficScene (Scene Graph Manager)
├── CoordinateTransform (Coordinate System)
├── Visual Objects
├── VisualEdge (Roads)
├── VisualLane (Individual Lanes)
└── VisualVehicle (Vehicles)
```

### Key Components

#### **TrafficSimulatorApp**
- Main JavaFX application class handling UI events and animation loop
- Manages zoom, pan, and coordinate transformation
- Coordinates between all subsystems with 60fps rendering

#### **NetworkParser**
- Parses SUMO network XML files (`.net.xml`)
- Extracts junctions, edges, and lane information
- Provides structured network data with bounds calculation

#### **SimulationRunner** 
- Interfaces with SUMO via TraaS library
- Manages simulation lifecycle and real-time data retrieval
- Provides vehicle position updates in background thread

#### **TrafficScene**
- Scene graph managing all visual objects
- Handles click detection and object selection
- Coordinates vehicle updates from simulation data

#### **Visual Objects**
- **VisualEdge**: Roads with multi-lane rendering and click detection
- **VisualLane**: Individual lanes with precise hit testing
- **VisualVehicle**: Interactive vehicles with type-specific appearance and behavior

#### **CoordinateTransform**
- Handles world-to-screen coordinate conversion with Y-axis flip
- Manages multi-level scaling: base scale + user zoom + pan offset
- Ensures consistent positioning across all visual objects

## 🎯 Coordinate System & Algorithms

### **Coordinate Transformation**

The application handles coordinate conversion between SUMO's world coordinates and JavaFX screen coordinates:

#### **SUMO Coordinate System**:
- **Origin**: Arbitrary world position
- **Units**: Meters
- **Y-Axis**: Up (North = positive Y)
- **Range**: Real-world coordinates (e.g., 1000-5000m)

#### **JavaFX Screen Coordinates**:
- **Origin**: Top-left corner (0,0)
- **Units**: Pixels
- **Y-Axis**: Down (South = positive Y)
- **Range**: Canvas size (e.g., 0-800 pixels)

#### **Multi-Level Transformation**:
```java
// World → Screen conversion with 3 levels:
screenX = (worldX * scale * zoom) + offsetX + panX
screenY = canvasHeight - ((worldY * scale * zoom) + offsetY + panY)

// Components:
// scale:   Base scaling to fit network (calculated once)
// zoom:    User zoom level (0.1x to 10x)
// offset:  Centering offset (calculated once)  
// pan:     User drag offset (updated on mouse drag)
```

### **Zoom-to-Point Algorithm**

The application implements intelligent zoom that keeps the point under the cursor/center stationary:

#### **Algorithm Steps**:

1. **Capture World Point** (before zoom change):
   ```java
   double oldScale = scale * zoom;
   double worldX = (targetX - offsetX - panX) / oldScale;
   double worldY = (targetY - offsetY - panY) / oldScale;
   ```

2. **Apply New Zoom Level**:
   ```java
   zoom = Math.max(0.1, Math.min(10.0, zoom * factor));
   double newScale = scale * zoom;
   ```

3. **Calculate Pan Adjustment**:
   ```java
   // Keep world point fixed at target screen position
   panX = targetX - offsetX - worldX * newScale;
   panY = targetY - offsetY - worldY * newScale;
   ```

#### **Zoom Behaviors**:

- **🖱️ Mouse Wheel**: Zooms toward cursor position (`e.getX(), e.getY()`)
- **🔘 Button Clicks**: Zooms toward screen center (`canvas.width/2, canvas.height/2`)
- **📐 Mathematics**: Same algorithm, different target points

### **Pan Algorithm**

Simple relative movement tracking:

```java
// On mouse press: capture starting positions
dragStartPanX = panX;
dragStartPanY = panY;

// On mouse drag: calculate relative movement
panX = dragStartPanX + (currentMouseX - dragStartMouseX);
panY = dragStartPanY - (currentMouseY - dragStartMouseY);  // Y-flip for natural panning
```

### **Vehicle Angle Correction**

SUMO and JavaFX use different angle conventions:

```java
// SUMO: 0° = North, 90° = East, clockwise
// JavaFX: 0° = East, 90° = South, clockwise  
// Y-axis flip affects rotation direction

double sumoAngle = vehicleData[2];           // From SUMO
double javaFXAngle = -(90.0 - sumoAngle);   // Convert for JavaFX
```

**Angle Mapping Examples:**
- SUMO North (0°) → JavaFX (-90°) = West
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
   git clone <repository-url>
   cd TraaShTrafficSimulator
   ```

2. **Install SUMO:**
   ```bash
   # Ubuntu/Debian
   sudo apt install sumo sumo-tools sumo-doc
   
   # Or download from https://sumo.dlr.de/docs/Installing/index.html
   ```

3. **Setup JavaFX:**
   - Download JavaFX SDK from https://openjfx.io/
   - Extract to `lib/javafx/` directory

4. **Verify directory structure:**
   ```
   TraaShTrafficSimulator/
   ├── src/                    # Source code
   ├── lib/
   │   ├── javafx/            # JavaFX libraries
   │   └── TraaS.jar          # SUMO integration
   ├── resource/              # SUMO configuration files
   │   ├── network.net.xml    # Network definition
   │   └── simulation.sumocfg # Simulation config
   └── bin/                   # Compiled classes
   ```

## 🚀 Usage

### Compilation
```bash
# Compile all Java source files
javac -cp ".:lib/*:lib/javafx/*" -d bin src/*.java
```

### Running the Application
```bash
# Launch the traffic simulator
java -cp "bin:lib/javafx/*:lib/TraaS.jar" \
     --module-path lib/javafx \
     --add-modules javafx.controls,javafx.fxml \
     TrafficSimulatorApp
```

### Controls
- **Mouse Wheel**: Zoom in/out toward cursor position
- **Mouse Drag**: Pan the view in natural direction
- **Mouse Click**: Select vehicles and lanes (see console output)
- **+ Button**: Zoom in toward screen center
- **- Button**: Zoom out toward screen center  
- **Reset Button**: Fit entire network to window with optimal scaling

## 🎯 Interaction Features

### Current Functionality
- **Vehicle Selection**: Click on vehicles to see ID and type information
- **Lane Detection**: Click on lanes to see lane identification
- **Real-time Updates**: Vehicle positions update from SUMO simulation at 60fps
- **Smooth Navigation**: Professional-quality zoom and pan with proper point-fixing

### Planned Features
- **Add Vehicle Dialog**: Click lane → choose vehicle type → inject into simulation
- **Lane Highlighting**: Hover effects for better interaction feedback
- **Vehicle Editing**: Modify vehicle properties (speed, route, destination)
- **Traffic Light Control**: Interactive traffic signal management
- **Route Visualization**: Display vehicle routes and destinations

## 📁 File Structure

```
src/
├── TrafficSimulatorApp.java    # Main application with UI and event handling
├── NetworkParser.java          # SUMO XML parser and data structures
├── SimulationRunner.java       # SUMO simulation interface via TraaS
├── TrafficScene.java          # Scene graph manager and hit detection
├── CoordinateTransform.java   # Coordinate system and transformations
├── VisualEdge.java            # Road visual representation with lanes
├── VisualLane.java            # Lane visual representation and interaction
├── VisualVehicle.java         # Vehicle visual representation and behavior
└── legacy/                    # Archived old implementation

resource/
├── network.net.xml            # SUMO network definition
└── simulation.sumocfg         # SUMO simulation configuration

lib/
├── javafx/                    # JavaFX runtime libraries
└── TraaS.jar                  # SUMO TraCI integration library
```

## 🔧 Configuration

### SUMO Network Files
- Place your `.net.xml` and `.sumocfg` files in the `resource/` directory
- Update file paths in `TrafficSimulatorApp.java` if needed:
  ```java
  private static final String NETWORK_FILE = "resource/your-network.net.xml";
  private static final String CONFIG_FILE = "resource/your-config.sumocfg";
  ```

### Display Settings
- Canvas size: 1000x800 pixels (configurable in `start()` method)
- Background color: Dark theme (RGB 20, 24, 28)
- Lane width: 3.2m (SUMO standard, configurable in VisualEdge)
- Zoom limits: 0.1x to 10x (configurable in zoom methods)

## 🐛 Troubleshooting

### Common Issues

**"Module javafx.controls not found"**
- Ensure JavaFX is properly installed in `lib/javafx/`
- Verify module path includes JavaFX directory
- Check JavaFX version compatibility with Java version

**"TraaS connection failed" or simulation not starting**
- Verify SUMO is installed and accessible in system PATH
- Check SUMO configuration files are valid XML
- Ensure network file path matches simulation config
- Test SUMO independently: `sumo-gui -c resource/simulation.sumocfg`

**"Network file not found"**
- Verify `.net.xml` file exists in `resource/` directory
- Check file permissions are readable
- Verify file path matches `NETWORK_FILE` constant

**Zoom/Pan not working smoothly**
- Check mouse wheel scroll direction settings
- Verify canvas has proper focus for events
- Test with different zoom factors if behavior seems off

**Visual rendering issues**
- Try "Reset" button to recalibrate view and scaling
- Check network coordinate bounds are reasonable (not extremely large/small)
- Verify lane width and scaling calculations for your network size

## 🤝 Contributing

### Development Setup
1. Fork the repository
2. Follow the installation steps above
3. Test with provided sample network or your own SUMO files
4. Follow the existing code style and architecture patterns

### Code Style Guidelines
- **Object-oriented design**: Keep visual objects separate from data objects
- **Clean methods**: Prefer shorter methods with clear single responsibilities  
- **Consistent naming**: Use descriptive variable names matching the domain
- **Documentation**: Add comments for complex algorithms (coordinate transforms, etc.)

### Contribution Process
1. Create a feature branch (`git checkout -b feature/amazing-feature`)
2. Make your changes following the architecture patterns
3. Test thoroughly with zoom, pan, and click interactions
4. Commit your changes (`git commit -m 'Add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request with description of changes and testing done

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- **SUMO Development Team** - For the excellent traffic simulation platform and documentation
- **TraaS Project Contributors** - For providing reliable SUMO-Java integration
- **JavaFX Community** - For the powerful graphics framework and examples
- **Open Source Community** - For inspiration and best practices in traffic simulation visualization

## 📞 Contact & Support

- **Issues**: Report bugs and feature requests via GitHub Issues
- **Documentation**: See inline code documentation for technical details  
- **SUMO Support**: Visit https://sumo.dlr.de/ for SUMO-specific questions

---

*Built with ❤️ for traffic simulation visualization and analysis*