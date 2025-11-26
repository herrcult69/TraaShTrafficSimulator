# TraaShTrafficSimulator - Milestone 1

Real-time SUMO traffic visualization with JavaFX using TraaS. Milestone 1 focuses on core visualization: displaying the road network and moving vehicles with a UI mockup.

## Table of Contents

- [Milestone 1 Overview](#milestone-1-overview)
- [System Requirements](#system-requirements)
- [Project Structure](#project-structure)
- [Class Hierarchy and Responsibilities](#class-hierarchy-and-responsibilities)
- [Installation](#installation)
- [Compilation](#compilation)
- [Running the Application](#running-the-application)
- [Configuration](#configuration)
- [Technical Specifications](#technical-specifications)
- [Troubleshooting](#troubleshooting)
- [Development Tools](#development-tools)
- [License](#license)

## Milestone 1 Overview

**Objective**: Basic traffic visualization without user interactions.

**Features Implemented**:
- ✅ SUMO network rendering (roads, junctions)
- ✅ Real-time vehicle visualization with type detection
- ✅ Automatic view centering and scaling
- ✅ Coordinate transformation (SUMO ↔ JavaFX)
- ✅ Background simulation thread
- ✅ Mockup UI panel (non-functional)
- ✅ Traffic light state debugging (terminal output)

**Features NOT Implemented** (Future Milestones):
- ❌ User interactions (zoom, pan, click)
- ❌ Functional control panel
- ❌ Dashboard statistics and charts
- ❌ Traffic light visualization on canvas

## System Requirements

### Software Dependencies
- **Java**: JDK 17+
- **JavaFX**: Version 17.0.2+
- **SUMO**: Version 1.20.0+
- **TraaS Library**: 1.0 (included in `lib/`)

### Operating Systems
- Linux (Tested on Ubuntu 20.04+)
- macOS (M1/M4)
- Windows 10/11

## Project Structure

```
TraaShTrafficSimulator/
├── src/                          # Source files (10 classes)
│   ├── TrafficSimulatorApp.java  # Main application entry point
│   ├── NetworkParser.java        # SUMO XML network parser
│   ├── SimulationRunner.java     # SUMO connection + TL debugging
│   ├── TraaSAdapter.java         # TraCI command wrapper
│   ├── CoordinateTransform.java  # Coordinate system converter
│   ├── DashBoard.java            # UI mockup panel
│   ├── TrafficManager.java       # Scene graph manager
│   ├── Edge.java                 # Road segment renderer
│   ├── Junction.java             # Intersection renderer
│   └── Vehicle.java              # Vehicle object and renderer
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
    └── createMap.sh              # SUMO network generation script
```

## Class Hierarchy and Responsibilities

### Application Layer
**TrafficSimulatorApp**
- Extends: `javafx.application.Application`
- Responsibilities: Application lifecycle, window setup, 60fps render loop, automatic view centering
- Key Methods: `start()`, `draw()`, `resetView()`
- **Milestone 1**: No mouse interactions was added yet.

### Data Layer
**NetworkParser**
- Static utility class
- Responsibilities: Parse SUMO network XML files (`.net.xml`)
- Inner Classes: `NetworkData`, `Junction`, `Edge`, `Lane`
- Returns: Structured network data with bounds (minX, maxX, minY, maxY)

**SimulationRunner**
- Implements: `Runnable`
- Responsibilities: Background thread managing SUMO connection, vehicle data collection, traffic light debugging
- Thread-Safe: Uses `ConcurrentHashMap` for vehicle positions
- Key Methods: `run()`, `stop()`, `interpretSignalChar()`
- **Debug Output**: Prints traffic light states every 20 steps with character-by-character interpretation

**TraaSAdapter**
- Wrapper class for TraaS library
- Responsibilities: Simplify TraCI command execution
- Key Methods: `getVehicleIds()`, `getVehiclePosition()`, `getVehicleAngle()`, `getTrafficLightIds()`, `getTrafficLightState()`

### View Layer
**CoordinateTransform**
- Utility class
- Responsibilities: Convert between SUMO world coordinates (Y-up, meters) and JavaFX screen coordinates (Y-down, pixels)
- Key Methods: `worldToScreenX()`, `worldToScreenY()`, `worldToScreenSize()`, `updateTransform()`
- Pan and Zoom will be implemented using this.

### UI Layer
**DashBoard**
- Mockup UI panel (non-functional)
- Components: Static labels for simulation time, vehicle counts, speed, and view controls
- Returns: `ScrollPane` containing mockup buttons and labels
- **Milestone 1**: Display only, no actual data updates or button functionality

### Rendering Layer
**TrafficManager**
- Responsibilities: Scene graph management, object lifecycle, rendering coordination
- Collections: Lists of `Junction`, `Edge`, `Vehicle` objects
- Key Methods: `initializeFromNetwork()`, `updateVehicles()`, `render()`

**Junction**
- Responsibilities: Render intersection polygons from SUMO shape data
- Key Methods: `render()`, `getRadius()`
- Geometry: Polygon rendering from SUMO shape coordinates
- Basic clipping of edge by providing arbitary radious values.

**Edge**
- Responsibilities: Road segment rendering with clipping at junctions
- Rendering: Solid gray road with yellow center line
- Key Methods: `render()`
- Uses standard 3.2m lane width

**Vehicle**
- Responsibilities: Vehicle visualization, type detection from ID prefix, position updates
- Properties: Type (car/truck/bus/motorcycle/emergency), dimensions, color
- Key Methods: `render()`, `updatePosition()`, `determineTypeFromId()`

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

## Milestone 1 Usage

### Display Mode
- Application launches in **display-only mode**
- Network automatically centered and scaled to fit canvas
- Vehicles update in real-time from SUMO simulation
- UI panel shows mockup controls (non-functional)

### Traffic Light Debugging (Terminal Output)
Traffic light states print to console every 1 second (20 steps):
```
=== TRAFFIC LIGHTS DETECTED ===
Total traffic lights: 4
  - Traffic Light ID: J1
================================

======= Traffic Light States at t=1.0s =======

Traffic Light: J1
  State String: 'GGrrrrGGrrrr' (length=12)
  Note: Each character = one link/movement through intersection

  Signal Breakdown (each signal controls one movement):
    [ 0] = 'G' → GREEN (go, priority)
    [ 1] = 'G' → GREEN (go, priority)
    [ 2] = 'r' → RED (stop)
    ...
```

### No User Interactions
**Milestone 1 does not support**:
- Mouse zoom/pan
- Click interactions
- Control panel buttons
- Dashboard updates

These features will be implemented in future milestones.

## Technical Specifications

### Coordinate System
- **SUMO**: Origin arbitrary, Y-axis up, units in meters
- **JavaFX**: Origin top-left (0,0), Y-axis down, units in pixels
- **Transformation**: Single-level (base scale + offsets for centering)
- **Centering Algorithm**: Calculates scaled dimensions and centers with equal margins

### Performance
- **Render Rate**: 60 FPS (JavaFX AnimationTimer)
- **Simulation Update**: ~20 Hz (50ms sleep intervals)
- **Threading**: Separate threads for UI (JavaFX) and simulation (ExecutorService)
- **Thread-Safety**: ConcurrentHashMap for vehicle data sharing

### Road Rendering
- **Lane Width**: Standardized to 3.2 meters
- **Road Surface**: Solid gray stroke (rgb(70, 70, 70))
- **Center Line**: Yellow stroke (rgb(255, 220, 50))
- **Junction Fill**: Dark gray (rgb(55, 60, 65))
- **Background**: Dark blue (rgb(0, 14, 36))

### Vehicle Detection
Vehicle types determined by ID prefix:
- `car*` → Car (4.5m × 1.8m, red)
- `truck*` → Truck (8m × 2.5m, blue)
- `bus*` → Bus (10m × 2.5m, green)
- `moto*` → Motorcycle (2m × 0.8m, orange-yellow)
- `ambu*` → Emergency (6m × 2.5m, grayish-white)

### Code Statistics (Milestone 1)
- **Total Lines**: 989
- **Classes**: 10
- **Removed from Original**: Lane tracking (~130 lines), click detection, zoom/pan logic, dashboard updates
- **Focus**: Core visualization with minimal complexity

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

