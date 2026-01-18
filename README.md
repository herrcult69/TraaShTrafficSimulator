# TraaShTrafficSimulator

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17-blue)
![SUMO](https://img.shields.io/badge/SUMO-1.24.0-green)
![Maven](https://img.shields.io/badge/Maven-3.6.0-red)
![License](https://img.shields.io/badge/License-MIT-yellow)

Real-time SUMO traffic visualization application with JavaFX. This application provides a graphical interface to visualize and interact with traffic simulations running on SUMO (Simulation of Urban MObility).

## Table of Contents

- [System Requirements](#system-requirements)
- [Project Structure](#project-structure)
- [Class Organization](#class-organization)
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
- **JavaFX**: Version 17.0.17 (including javafx-swing module)
- **Maven**: Version 3.6.0+
- **SUMO**: Version 1.24.0
- **TraaS Library**: 1.0 (included in `lib/`)
- **Apache PDFBox**: 2.0.30 (managed by Maven)

### Operating Systems

- Linux (Tested on Ubuntu 20.04+)
- macOS (M4)
- Windows 10/11

## Project Structure

```
TraaShTrafficSimulator/
├── src/                                # Source files
│   ├── TrafficSimulatorApp.java        # Main application entry point
│   ├── NetworkParser.java              # SUMO XML network parser
│   ├── SimulationRunner.java           # SUMO connection manager (background thread)
│   ├── TraaSAdapter.java               # TraCI command wrapper
│   ├── CoordinateTransform.java        # Coordinate system converter
│   ├── ViewManager.java                # Zoom and pan controller
│   ├── ControlPanel.java               # UI controls (play/pause/zoom)
│   ├── TrafficLightControlPanel.java   # Traffic light management UI
│   ├── VehicleAddPanel.java            # Vehicle spawning UI
│   ├── VehicleFilterPanel.java         # Vehicle type filtering UI
│   ├── DashBoard.java                  # Statistics display panel
│   ├── StatisticsWindow.java           # Advanced analytics window
│   ├── CongestionMonitorPanel.java     # Congestion monitoring UI
│   ├── TrafficDataExporter.java        # CSV and PDF export utilities
│   ├── UIStyles.java                   # UI styling constants
│   ├── TrafficManager.java             # Scene graph manager
│   ├── Edge.java                       # Road segment renderer
│   ├── Lane.java                       # Individual lane renderer
│   ├── Junction.java                   # Intersection renderer
│   ├── Vehicle.java                    # Vehicle object and renderer
│   ├── TrafficLight.java               # Traffic signal renderer
│   ├── CongestionHotspot.java          # Congestion detection and scoring
│   └── VehicleTypeHelper.java          # Vehicle type detection utility
├── lib/
│   ├── javafx/                         # JavaFX SDK libraries
│   └── TraaS.jar                       # SUMO TraCI Java library
├── resource/
│   ├── network.net.xml                 # SUMO network definition
│   ├── simulation.sumocfg              # SUMO simulation configuration
│   ├── cars.rou.xml                    # Car routes
│   ├── trucks.rou.xml                  # Truck routes
│   ├── buses.rou.xml                   # Bus routes
│   ├── motorcycles.rou.xml             # Motorcycle routes
│   └── emergency.rou.xml               # Emergency vehicle routes
└── tools/                              # Helper scripts
    └── createMap.sh                    # Map generation script
```

## Class Organization

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

**StatisticsWindow**

- Extends: `Stage`
- Responsibilities: Advanced traffic analytics with multiple synchronized charts
- Components: 4 charts (speed over time, vehicle count by type, travel time distribution, distance distribution)
- Features: Real-time updates, PDF export, resizable window
- Update Frequency: 2 times per second (0.5s intervals)

**VehicleFilterPanel**

- Responsibilities: Vehicle type visibility controls, speed-based color filtering
- Components: Checkboxes for each vehicle type, speed filter toggle
- Integration: Filters apply to visualization and statistics

**CongestionMonitorPanel**

- Extends: `VBox`
- Responsibilities: Display congestion hotspots, overlay controls
- Components: Top hotspot ranking list, severity indicators, toggle overlay button
- Features: Real-time congestion tracking, color-coded severity levels

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

**CongestionHotspot**

- Responsibilities: Congestion detection, severity calculation, visual overlay rendering
- Algorithm: Density and speed-based scoring (0-100 scale)
- Properties: Severity level (1-5), congestion score, duration, average speed, vehicle density
- Key Methods: `updateMetrics()`, `render()`, `getSeverityColor()`, `getSeverityDescription()`

**TrafficDataExporter**

- Static utility class
- Responsibilities: Export simulation data to CSV and PDF formats
- Key Methods: `exportToCSV()`, `exportPDF()`
- Features: File chooser integration, formatted PDF output with charts

**VehicleTypeHelper**

- Static utility class
- Responsibilities: Vehicle type detection from vehicle ID
- Key Methods: `getVehicleType()`

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

### 2. Install Maven

**Linux (Ubuntu/Debian):**

```bash
sudo apt install maven
mvn -version
```

**macOS:**

```bash
brew install maven
mvn -version
```

**Windows:**
Download from [Maven Apache](https://maven.apache.org/download.cgi)
Add `bin` directory to PATH

### 3. Install SUMO

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

### 4. Install JavaFX (Optional if using Maven)

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

### 5. Clone Repository

```bash
git clone git@github.com:herrcult69/TraaShTrafficSimulator.git
cd TraaShTrafficSimulator
```

### 6. Verify Structure

Ensure the following directories exist:

- `src/` (Java source files)
- `lib/javafx/` (JavaFX libraries)
- `lib/TraaS.jar` (included)
- `resource/` (SUMO configuration files)

## Compilation

You can compile the project using Maven, provided helper scripts, or manually.

### Option 1: Maven (Recommended)

Works on all platforms (Windows, macOS, Linux).

```bash
mvn clean compile
```

### Option 2: Helper Scripts

**Windows:**

```batch
compile.bat
```

**Linux / macOS:**

```bash
make compile
```

### Option 3: Manual Command Line

**Windows:**

```cmd
if not exist bin mkdir bin
javac -cp "lib\TraaS.jar;lib\pdfbox-2.0.30.jar;lib\fontbox-2.0.30.jar;lib\commons-logging-1.2.jar;lib\javafx\*" -d bin src\*.java
```

**Linux / macOS:**

```bash
mkdir -p bin
javac -cp "lib/TraaS.jar:lib/pdfbox-2.0.30.jar:lib/fontbox-2.0.30.jar:lib/commons-logging-1.2.jar:lib/javafx/*" -d bin src/*.java
```

## Running the Application

### Option 1: Maven (Reommended)

```bash
mvn javafx:run
```

### Option 2: Helper Scripts

**Windows:**

```batch
run.bat
```

**Linux / macOS:**

```bash
make run
```

### Option 3: Manual Command Line

**Windows:**

```cmd
java --module-path "lib\javafx" ^
     --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing ^
     -cp "lib\TraaS.jar;lib\pdfbox-2.0.30.jar;lib\fontbox-2.0.30.jar;lib\commons-logging-1.2.jar;bin" ^
     TrafficSimulatorApp
```

**Linux / macOS:**

```bash
java -cp "bin:lib/TraaS.jar:lib/pdfbox-2.0.30.jar:lib/fontbox-2.0.30.jar:lib/commons-logging-1.2.jar:lib/javafx/*" \
     --module-path lib/javafx \
     --add-modules javafx.controls,javafx.fxml,javafx.swing \
     TrafficSimulatorApp
```

## Configuration

### Network Files

Place custom SUMO files in `SumoConfig/` directory:

- `network.net.xml` - Road network (created with netedit or netgenerate)
- `simulation.sumocfg` - Main configuration file
- `*.rou.xml` - Route files for different vehicle types

**If network or route files do not exist**, use the provided script to generate them:

**Linux / macOS:**

```bash
cd tools
./createMap.sh
```

**Windows (Git Bash):**

```bash
cd tools
bash createMap.sh
```

This script will automatically:
- Generate a random road network with traffic lights
- Create route files for all vehicle types (cars, trucks, buses, motorcycles, emergency)
- Generate the SUMO configuration file
- Set up the complete simulation environment in `SumoConfig/` directory

**Script Parameters** (optional, edit in `createMap.sh`):
- `RAND_ITERATIONS`: Network complexity (default: 35)
- `RAND_MAX_DISTANCE`: Maximum edge length (default: 110m)
- `CAR_PERIOD`, `TRUCK_PERIOD`, etc.: Vehicle spawn frequency in seconds

### Application Settings

Edit constants in `TrafficSimulatorApp.java`:

```java
private static final String NETWORK_FILE = "SumoConfig/network.net.xml";
private static final String CONFIG_FILE = "SumoConfig/simulation.sumocfg";
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

- **[\>] Play**: Resume simulation
- **|\=| Pause**: Pause simulation (navigation still active)
- **[\#] Stop**: Stop simulation and exit application
- **[\@] Add Vehicle**: Vehicle Injection- ** View Statistics**: Open advanced analytics window with live charts
- ** Export Data**: Export current simulation data to CSV file
- ** Vehicle Filter**: Toggle visibility and filtering for vehicle types
- ** Congestion Monitor**: View congestion hotspots and overlay controls- **+ Zoom In**: Zoom toward screen center
- **− Zoom Out**: Zoom away from screen center
- **⟲ Reset View**: Fit entire network to window

### Dashboard Metrics

- Simulation time (seconds)
- Active vehicle count
- Average speed (m/s)
- Vehicle type breakdown (cars, trucks, buses, motorcycles, emergency)
- Real-time speed chart (60-second window)

### Statistics Window

- **Average Speed Over Time**: Line chart with 120-second sliding window
- **Vehicle Count by Type**: Bar chart showing current distribution
- **Travel Time Distribution**: Histogram with 30-second bins (0-30s, 30-60s, 60-90s, 90-120s, 120-150s, 150+s)
- **Distance Traveled Distribution**: Histogram with 200-meter bins (0-200m, 200-400m, 400-600m, 600-800m, 800-1000m, 1000m+)
- **Export to PDF**: Save all charts to formatted PDF document

### Vehicle Filter Panel

- **Type Filters**: Toggle visibility for cars, trucks, buses, motorcycles, and emergency vehicles
- **Speed Filter**: Enable speed-based color coding (Green=Slow, Yellow=Medium, Red=Fast)
- Filters apply to both visualization and statistics data

### Congestion Monitor

- **Top Hotspots List**: Shows top 5 worst congestion points with detailed metrics
- **Severity Levels**: Light (green), Moderate (yellow-green), Heavy (yellow), Severe (orange), Critical (red)
- **Metrics Displayed**: Edge ID, congestion score, average speed, vehicle density, duration
- **Overlay Toggle**: Show/hide color-coded congestion overlays on the map
- **Active Hotspot Count**: Total number of congested edges

## Technical Specifications

### Coordinate System

- **SUMO**: Origin arbitrary, Y-axis up, units in meters
- **JavaFX**: Origin top-left (0,0), Y-axis down, units in pixels
- **Transformation**: Multi-level (base scale × zoom + offsets)

### Performance

- **Render Rate**: 60 FPS (JavaFX AnimationTimer)
- **Simulation Update**: 20 Hz (50ms intervals)
- **Dashboard Update**: 2 Hz (0.5s intervals)
- **Statistics Window Update**: 2 Hz (0.5s intervals)
- **Congestion Detection**: Real-time with each render cycle
- **Threading**: Separate threads for UI and simulation

### Congestion Detection Algorithm

- **Density Thresholds**: 
  - High: 35 vehicles/km
  - Moderate: 10 vehicles/km
- **Speed Thresholds**:
  - Low: 5 m/s (18 km/h)
  - Moderate: 10 m/s (36 km/h)
- **Minimum Vehicle Count**: 3 vehicles (prevents false positives)
- **Scoring Formula**: Weighted combination (40% speed factor, 60% density factor)
- **Severity Levels**:
  - Level 1 (Light): Score 10-20
  - Level 2 (Moderate): Score 20-40
  - Level 3 (Heavy): Score 40-60
  - Level 4 (Severe): Score 60-80
  - Level 5 (Critical): Score 80-100

### Vehicle Detection

Vehicle types determined by ID prefix:

- `car*` → Car (4.5m × 1.8m, red)
- `truck*` → Truck (6m × 2.5m, blue)
- `bus*` → Bus (8m × 2.5m, green)
- `moto*` → Motorcycle (2m × 0.8m, orangie-yellow)
- `ambu*` → Emergency (6m × 2.5m, greyish-white)

### Interactable Objects

Currently _lanes_, _junctions_ and _vehicles_ are all interactable.

- They will provide their information upon click in the terminal
- `Note*` that the vehicle's hitbox is a circle at its HEAD in the direction of travel.

### Data Export Formats

**CSV Export** includes:
- Simulation time
- Total vehicle count
- Vehicle counts by type (cars, trucks, buses, motorcycles, emergency)
- Average speed (m/s)

**PDF Export** includes:
- All four statistics charts on A4 page
- Formatted title header
- Automatically scaled to fit page while maintaining aspect ratio

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
Error: SumoConfig/network.net.xml not found
```

**Solution**: Ensure network file exists and path in `TrafficSimulatorApp.java` is correct

### Canvas Not Resizing

**Solution**: Restart application. Canvas binds to window size on startup.

### Batch Files Not Working (Windows)

**Error**: `javac: file not found` or `Could not find or load main class`

**Solution**:

1.  **Preferred**: Extract the JavaFX SDK to the `lib\javafx\` directory in the project root. The scripts are configured to look there.
2.  **Ensure all required JARs are present** in `lib/` directory:
    - `TraaS.jar`
    - `pdfbox-2.0.30.jar`
    - `fontbox-2.0.30.jar`
    - `commons-logging-1.2.jar`
3.  **Alternative**: Edit `compile.bat` and `run.bat` to point to your specific JavaFX installation path (e.g., replace `lib\javafx` with `C:\Path\To\JavaFX\lib`).

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
