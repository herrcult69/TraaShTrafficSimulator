# TraaShTrafficSimulator

Real-time SUMO traffic visualization application with JavaFX.

## Table of Contents

- [System Requirements](#system-requirements)
- [Project Structure](#project-structure)
- [Class Hierarchy and Responsibilities](#class-hierarchy-and-responsibilities)
- [Installation](#installation)
- [Compilation](#compilation)
- [Running the Application](#running-the-application)
- [Configuration](#configuration)
- [Usage](#usage)
- [Technical Specifications](#technical-specifications)
- [Troubleshooting](#troubleshooting)
- [Development Tools](#development-tools)
- [License](#license)


## System Requirements

### Software Dependencies
- **Java**: JDK 17 
- **JavaFX**: Version 17.0.17
- **SUMO**: Version 1.24.0 
- **TraaS Library**: 1.0 (included in `lib/`)

### Operating Systems
- Linux (Tested on Ubuntu 20.04+)
- macOS (M4)
- Windows 10/11

## Project Structure

```
TraaShTrafficSimulator/
├── src/                          # Source files
│   ├── TrafficSimulatorApp.java  # Main application entry point
│   ├── NetworkParser.java        # SUMO XML network parser
│   ├── SimulationRunner.java     # SUMO connection manager (background thread)
│   ├── TraaSAdapter.java         # TraCI command wrapper
│   ├── CoordinateTransform.java  # Coordinate system converter
│   ├── ViewManager.java          # Zoom and pan controller
│   ├── ControlPanel.java         # UI controls (play/pause/zoom)
│   ├── DashBoard.java            # Statistics display panel
│   ├── TrafficManager.java       # Scene graph manager
│   ├── Edge.java                 # Road segment renderer
│   ├── Lane.java                 # Individual lane renderer
│   ├── Junction.java             # Intersection renderer
│   ├── Vehicle.java              # Vehicle object and renderer
│   └── TrafficLight.java         # Traffic signal renderer
├── lib/
│   ├── javafx/                   # JavaFX SDK libraries
│   └── TraaS.jar                 # SUMO TraCI Java library
├── resource/
│   ├── network.net.xml           # SUMO network definition
│   ├── simulation.sumocfg        # SUMO simulation configuration
│   ├── cars.rou.xml              # Car routes
│   ├── trucks.rou.xml            # Truck routes
│   ├── buses.rou.xml             # Bus routes
│   ├── motorcycles.rou.xml       # Motorcycle routes
│   └── emergency.rou.xml         # Emergency vehicle routes
└── tools/                        # Helper scripts
```

## Class Hierarchy and Responsibilities

### Application Layer
**TrafficSimulatorApp**
- Extends: `javafx.application.Application`
- Responsibilities: Application lifecycle, window setup, render loop (60fps), mouse event handling
- Key Methods: `start()`, `render()`, `setupMouseHandlers()`

### Data Layer
**NetworkParser**
- Static utility class
- Responsibilities: Parse SUMO network XML files (`.net.xml`)
- Inner Classes: `NetworkData`, `Junction`, `Edge`, `Lane`
- Returns: Structured network data with bounds

**SimulationRunner**
- Implements: `Runnable`
- Responsibilities: Background thread managing SUMO connection, timestep advancement, vehicle data collection
- Thread-Safe: Uses `ConcurrentHashMap` for vehicle positions/speeds
- Key Methods: `run()`, `pause()`, `resume()`, `stop()`

**TraaSAdapter**
- Wrapper class for TraaS library
- Responsibilities: Simplify TraCI command execution
- Key Methods: `getVehicleIds()`, `getVehiclePosition()`, `getVehicleSpeed()`, `getVehicleAngle()`

### View Layer
**ViewManager**
- Responsibilities: View transformation state management (zoom, pan, scale)
- Key Methods: `resetView()`, `zoomToCenter()`, `zoomToPoint()`, `startPan()`, `updatePan()`
- State Variables: `scale`, `zoom`, `offsetX`, `offsetY`, `panX`, `panY`

**CoordinateTransform**
- Utility class
- Responsibilities: Convert between SUMO world coordinates (Y-up, meters) and JavaFX screen coordinates (Y-down, pixels)
- Key Methods: `worldToScreenX()`, `worldToScreenY()`, `screenToWorldX()`, `screenToWorldY()`

### UI Layer
**ControlPanel**
- Extends: None (composes `VBox`)
- Responsibilities: Simulation controls (play/pause/stop), view controls (zoom/reset)
- Returns: `ScrollPane` containing control buttons

**DashBoard**
- Extends: `VBox`
- Responsibilities: Display real-time statistics, speed chart (60-second window)
- Components: Labels for metrics, `LineChart` for speed visualization
- Update Frequency: 2 times per second (0.5s intervals)

### Rendering Layer
**TrafficManager**
- Responsibilities: Scene graph management, object lifecycle, click detection
- Collections: Lists of `Junction`, `Edge`, `Vehicle` objects
- Key Methods: `initializeFromNetwork()`, `updateVehicles()`, `render()`, `getElementAt()`

**Junction**
- Responsibilities: Render intersection polygons from SUMO shape data
- Key Methods: `render()`, `getRadiusInDirection()`
- Geometry: Polygon rendering from SUMO shape coordinates

**Edge**
- Responsibilities: Road segment rendering, lane creation, lane marking visualization
- Contains: Multiple `Lane` objects (bidirectional)
- Key Methods: `render()`, `getLaneAt()`, `createLanes()`

**Lane**
- Responsibilities: Individual lane rendering, hit detection
- Properties: Width, offset from edge centerline, index
- Key Methods: `render()`, `contains()`

**Vehicle**
- Responsibilities: Vehicle visualization, type detection, position updates
- Properties: Type (car/truck/bus/motorcycle/emergency), dimensions, color
- Key Methods: `render()`, `updatePosition()`, `contains()`

**TrafficLight**
- Responsibilities: Traffic signal state visualization
- Status: Partially implemented

## Installation

### 1. Install Java JDK

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
java --version
```

**macOS:**
```bash
brew install openjdk@17
java --version
```

**Windows:**
Download and install from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [AdoptOpenJDK](https://adoptium.net/)

### 2. Install SUMO

**Linux (Ubuntu/Debian):**
```bash
sudo add-apt-repository ppa:sumo/stable
sudo apt update
sudo apt install sumo sumo-tools sumo-doc
sumo --version
```

**macOS:**
```bash
brew install sumo
sumo --version
```

**Windows:**
Download installer from [SUMO Downloads](https://sumo.dlr.de/docs/Downloads.php)
Add SUMO to system PATH

### 3. Install JavaFX

**All Platforms:**
1. Download JavaFX SDK 17+ from [https://openjfx.io/](https://openjfx.io/)
2. Extract to `lib/javafx/` directory in project root

**Directory structure after extraction:**
```
lib/javafx/
├── javafx.base.jar
├── javafx.controls.jar
├── javafx.fxml.jar
├── javafx.graphics.jar
└── ...
```

### 4. Clone Repository

```bash
git clone git@github.com:herrcult69/TraaShTrafficSimulator.git
cd TraaShTrafficSimulator
```

### 5. Verify Structure

Ensure the following directories exist:
- `src/` (Java source files)
- `lib/javafx/` (JavaFX libraries)
- `lib/TraaS.jar` (included)
- `resource/` (SUMO configuration files)

## Compilation

### Linux / macOS

**Using Makefile:**
```bash
make clean
make compile
```

**Manual compilation:**
```bash
mkdir -p bin
javac -cp ".:lib/*:lib/javafx/*" -d bin src/*.java
```

### Windows

**Using batch script:**
```batch
clean.bat
compile.bat
```

**Manual compilation:**
```cmd
if not exist bin mkdir bin
javac -cp "lib\*;lib\javafx\*" -d bin src\*.java
```

**Note**: Batch files use relative paths (`lib\javafx\`) and will work on any Windows system as long as JavaFX is extracted to the correct location.

## Running the Application

### Linux / macOS

**Using Makefile:**
```bash
make run
```

**Manual execution:**
```bash
java -cp "bin:lib/javafx/*:lib/TraaS.jar" \
     --module-path lib/javafx \
     --add-modules javafx.controls,javafx.fxml \
     TrafficSimulatorApp
```

### Windows

**Using batch script:**
```batch
run.bat
```

**Manual execution:**
```cmd
java --module-path "lib\javafx" ^
     --add-modules javafx.controls,javafx.fxml,javafx.graphics ^
     -cp "lib\TraaS.jar;bin" ^
     TrafficSimulatorApp
```

**Note**: Ensure JavaFX is extracted to `lib\javafx\` directory before running.

## Configuration

### Network Files

Place custom SUMO files in `resource/` directory:
- `network.net.xml` - Road network (created with netedit or netgenerate)
- `simulation.sumocfg` - Main configuration file
- `*.rou.xml` - Route files for different vehicle types

### Application Settings

Edit constants in `TrafficSimulatorApp.java`:
```java
private static final String NETWORK_FILE = "resource/network.net.xml";
private static final String CONFIG_FILE = "resource/simulation.sumocfg";
```

Edit view settings in `ViewManager.java`:
```java
private static final double MIN_ZOOM = 0.1;
private static final double MAX_ZOOM = 10.0;
```

## Usage

### Mouse Controls
- **Scroll Wheel**: Zoom in/out (focused on cursor position)
- **Click + Drag**: Pan view
- **Click Vehicle**: Select and print information to console

### Control Panel
- **▶ Play**: Resume simulation
- **|| Pause**: Pause simulation (navigation still active)
- **[] Stop**: Stop simulation and exit application
- **+ Zoom In**: Zoom toward screen center
- **− Zoom Out**: Zoom away from screen center
- **⟲ Reset View**: Fit entire network to window

### Dashboard Metrics
- Simulation time (seconds)
- Active vehicle count
- Average speed (m/s)
- Vehicle type breakdown (cars, trucks, buses, motorcycles, emergency)
- Real-time speed chart (60-second window)

## Technical Specifications

### Coordinate System
- **SUMO**: Origin arbitrary, Y-axis up, units in meters
- **JavaFX**: Origin top-left (0,0), Y-axis down, units in pixels
- **Transformation**: Multi-level (base scale × zoom + offsets)

### Performance
- **Render Rate**: 60 FPS (JavaFX AnimationTimer)
- **Simulation Update**: 20 Hz (50ms intervals)
- **Dashboard Update**: 2 Hz (0.5s intervals)
- **Threading**: Separate threads for UI and simulation

### Vehicle Detection
Vehicle types determined by ID prefix:
- `car*` → Car (4.5m × 1.8m, red)
- `truck*` → Truck (8m × 2.5m, blue)
- `bus*` → Bus (10m × 2.5m, green)
- `moto*` → Motorcycle (2m × 0.8m, orangie-yellow)
- `ambu*` → Emergency (6m × 2.5m, greyish-white)

### Interactable Objects
Currently the *lanes* and the *vehicles* are all interactable.
- They will provide their information upon click in the terminal
- `Note*` that the vehile's hitbox is a circle at its HEAD in the direction of travel.

## Troubleshooting

### JavaFX Module Not Found
```
Error: Module javafx.controls not found
```
**Solution**: Verify JavaFX is in `lib/javafx/` and module path is correct in run command

### SUMO Connection Failed
```
Error: Could not connect to SUMO
```
**Solution**: 
1. Verify SUMO is installed: `sumo --version`
2. Check SUMO is in system PATH
3. Test configuration: `sumo-gui -c resource/simulation.sumocfg`
4. Verify network file path in `.sumocfg` matches actual file

### Network File Not Found
```
Error: resource/network.net.xml not found
```
**Solution**: Ensure network file exists and path in `TrafficSimulatorApp.java` is correct

### Canvas Not Resizing
**Solution**: Restart application. Canvas binds to window size on startup.

### Batch Files Not Working (Windows)
```
Error: javac: file not found: src\*.java
Error: Could not find or load main class
```
**Problem**: JavaFX not in `lib\javafx\` or installed in different location

**Solution**: 
1. **Preferred**: Extract JavaFX SDK to `lib\javafx\` directory so batch files work without modification
2. **Alternative**: If JavaFX is installed elsewhere, edit the batch files to use absolute paths:

**In compile.bat**, change:
```batch
javac -cp "lib\*;lib\javafx\*" -d bin src\*.java
```
To:
```batch
javac -cp "lib\*;C:\path\to\your\javafx-sdk\lib\*" -d bin src\*.java
```

**In run.bat**, change:
```batch
java --module-path "lib\javafx" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "lib\TraaS.jar;bin" TrafficSimulatorApp
```
To:
```batch
java --module-path "C:\path\to\your\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "lib\TraaS.jar;bin" TrafficSimulatorApp
```

Replace `C:\path\to\your\javafx-sdk\lib` with your actual JavaFX installation path (e.g., `C:\Program Files\javafx-sdk-17.0.2\lib` or `D:\Downloads\javafx-sdk-17\lib`)

### Poor Performance
**Symptoms**: Lag, low FPS with many vehicles
**Solutions**:
- Reduce vehicle count in route files
- Disable dashboard updates (comment out dashboard.update() calls)
- Run SUMO in headless mode (default)

## Development Tools

### Creating SUMO Networks
```bash
# Interactive editor
netedit

# Generate random network
netgenerate --grid --grid.number=5 --output-file=network.net.xml

# Create routes
python $SUMO_HOME/tools/randomTrips.py -n network.net.xml -o trips.trips.xml
```

### Building with Make (Linux/macOS)
```bash
make clean      # Remove compiled files
make compile    # Compile source
make run        # Run application
make all        # Clean, compile, and run
```

## License

MIT License - See LICENSE file for details

