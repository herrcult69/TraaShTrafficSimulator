#!/usr/bin/env bash

# SUMO Network and Traffic Generation Script
# Generates a complete simulation setup with mixed vehicle types

set -euo pipefail  # Exit on error, treat unset vars as errors, fail on pipe errors

# helper for errors
die() {
    echo "ERROR: $*" 1>&2
    exit 1
}
#
# Choose a python executable (prefer python3)
if command -v python3 >/dev/null 2>&1; then
    PYTHON=python3
elif command -v python >/dev/null 2>&1; then
    PYTHON=python
else
    die "No python executable found (need python or python3 in PATH)"
fi

# If SUMO_HOME not set, try a common Windows path (works in Git Bash)
if [ -z "${SUMO_HOME:-}" ]; then
    if [ -d "/c/Program Files (x86)/Eclipse/Sumo" ]; then
        SUMO_HOME="/c/Program Files (x86)/Eclipse/Sumo"
        export SUMO_HOME
    fi
fi

if [ -z "${SUMO_HOME:-}" ]; then
    die "SUMO_HOME is not set. Please set SUMO_HOME to your SUMO installation root."
fi

if [ ! -f "$SUMO_HOME/tools/randomTrips.py" ]; then
    die "randomTrips.py not found at $SUMO_HOME/tools/randomTrips.py"
fi

# Make sure SUMO tools are on PYTHONPATH so randomTrips can import sumolib/traci
export PYTHONPATH="$SUMO_HOME/tools:${PYTHONPATH:-}"

RESOURCE_DIR="SumoConfig"
NETWORK_FILE="$RESOURCE_DIR/network.net.xml"
CONFIG_FILE="$RESOURCE_DIR/simulation.sumocfg"
SIMULATION_TIME=3600  # 1 hour in seconds

# Network parameters (RANDOM NETWORK)
RAND_ITERATIONS=20      # Number of random network iterations
RAND_MAX_DISTANCE=100   # Maximum edge length in meters
RAND_MIN_DISTANCE=50    # Minimum edge length in meters
DEFAULT_SPEED=10.89      # m/s
NUM_TRIES=200 
# Traffic density (seconds between vehicles)
# Lower values = more vehicles spawned
CAR_PERIOD=10          # Cars every 5 seconds (was 100)
TRUCK_PERIOD=40       # Trucks every 40 seconds (was 6000)
MOTORCYCLE_PERIOD=15  # Motorcycles every 15 seconds (was 1000)
BUS_PERIOD=30         # Buses every 30 seconds (was 650)
EMERGENCY_PERIOD=60   # Emergency vehicles every 60 seconds (was 900) 

# Create resource directory

mkdir -p "$RESOURCE_DIR"
echo "SUMO Simulation Generator (Random Network)"


# ====================
# Step 1: Generate Random Network
# ====================
echo ""
echo "[1/4] Generating random road network..."
netgenerate --rand \
    --rand.iterations=$RAND_ITERATIONS \
    --rand.num-tries=$NUM_TRIES \
    --rand.max-distance=$RAND_MAX_DISTANCE \
    --rand.min-distance=$RAND_MIN_DISTANCE \
    --rand.connectivity=0.8 \
    --tls.guess \
    --default.speed=$DEFAULT_SPEED \
    --default.lanenumber=2 \
    --junctions.join \
    --junctions.join-dist=10 \
    --output-file="$NETWORK_FILE"

if [ $? -eq 0 ]; then
    TLS_COUNT=$(grep -c "<tlLogic" "$NETWORK_FILE" || echo "0")
    EDGE_COUNT=$(grep -c "<edge id" "$NETWORK_FILE" || echo "0")
    JUNCTION_COUNT=$(grep -c "<junction id" "$NETWORK_FILE" || echo "0")
    echo "Random network created:"
    echo "Junctions: $JUNCTION_COUNT"
    echo "Edges: $EDGE_COUNT"
    echo "Traffic lights: $TLS_COUNT"
else
    echo "Network generation failed!"
    exit 1
fi


# Step 2: Generate Traffic for Each Vehicle Type

echo ""
echo "[2/4] Generating traffic routes..."

# Cars
echo "  - Generating cars (period: ${CAR_PERIOD}s)..."
"$PYTHON" "$SUMO_HOME/tools/randomTrips.py" \
    -n "$NETWORK_FILE" \
    -r "$RESOURCE_DIR/cars.rou.xml" \
    -e "$SIMULATION_TIME" \
    --period "$CAR_PERIOD" \
    --fringe-factor 10 \
    --min-distance 250 \
    --vehicle-class passenger \
    --prefix car \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\" color=\"1,0,0\"" \
    --validate

# Trucks
echo "  - Generating trucks (period: ${TRUCK_PERIOD}s)..."
"$PYTHON" "$SUMO_HOME/tools/randomTrips.py" \
    -n "$NETWORK_FILE" \
    -r "$RESOURCE_DIR/trucks.rou.xml" \
    -e "$SIMULATION_TIME" \
    --period "$TRUCK_PERIOD" \
    --fringe-factor 5 \
    --min-distance 300 \
    --vehicle-class truck \
    --prefix truck \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\" color=\"0,0,1\"" \
    --validate

# Motorcycles
echo "  - Generating motorcycles (period: ${MOTORCYCLE_PERIOD}s)..."
"$PYTHON" "$SUMO_HOME/tools/randomTrips.py" \
    -n "$NETWORK_FILE" \
    -r "$RESOURCE_DIR/motorcycles.rou.xml" \
    -e "$SIMULATION_TIME" \
    --period "$MOTORCYCLE_PERIOD" \
    --fringe-factor 5 \
    --min-distance 100 \
    --vehicle-class motorcycle \
    --prefix moto \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\" color=\"1,0,0\"" \
    --validate

# Buses
echo "  - Generating buses (period: ${BUS_PERIOD}s)..."
"$PYTHON" "$SUMO_HOME/tools/randomTrips.py" \
    -n "$NETWORK_FILE" \
    -r "$RESOURCE_DIR/buses.rou.xml" \
    -e "$SIMULATION_TIME" \
    --period "$BUS_PERIOD" \
    --fringe-factor 10 \
    --min-distance 250 \
    --vehicle-class bus \
    --prefix bus \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\" color=\"0,1,0\"" \
    --validate

# Emergency vehicles
echo "  - Generating emergency vehicles (period: ${EMERGENCY_PERIOD}s)..."
"$PYTHON" "$SUMO_HOME/tools/randomTrips.py" \
    -n "$NETWORK_FILE" \
    -r "$RESOURCE_DIR/emergency.rou.xml" \
    -e "$SIMULATION_TIME" \
    --period "$EMERGENCY_PERIOD" \
    --fringe-factor 5 \
    --min-distance 600 \
    --vehicle-class emergency \
    --prefix ambulance \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\" color=\"1,1,0\"" \
    --validate

echo "Traffic routes generated"


# # Step 3: Count Generated Vehicles

echo ""
echo "[3/4] Traffic statistics:"
CAR_COUNT=$(grep -c "<vehicle" "$RESOURCE_DIR/cars.rou.xml" || echo "0")
TRUCK_COUNT=$(grep -c "<vehicle" "$RESOURCE_DIR/trucks.rou.xml" || echo "0")
MOTO_COUNT=$(grep -c "<vehicle" "$RESOURCE_DIR/motorcycles.rou.xml" || echo "0")
BUS_COUNT=$(grep -c "<vehicle" "$RESOURCE_DIR/buses.rou.xml" || echo "0")
EMERGENCY_COUNT=$(grep -c "<vehicle" "$RESOURCE_DIR/emergency.rou.xml" || echo "0")
TOTAL=$((CAR_COUNT + TRUCK_COUNT + MOTO_COUNT + BUS_COUNT + EMERGENCY_COUNT))

echo "  Cars:        $CAR_COUNT"
echo "  Trucks:      $TRUCK_COUNT"
echo "  Motorcycles: $MOTO_COUNT"
echo "  Buses:       $BUS_COUNT"
echo "  Emergency:   $EMERGENCY_COUNT"
echo "  -------------------"
echo "  Total:       $TOTAL vehicles"


# Step 4: Create Configuration File

echo ""
echo "[4/4] Creating simulation configuration..."

cat > "$CONFIG_FILE" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <input>
        <net-file value="network.net.xml"/>
        <route-files value="cars.rou.xml,trucks.rou.xml,motorcycles.rou.xml,buses.rou.xml,emergency.rou.xml"/>
    </input>
    
    <time>
        <begin value="0"/>
        <end value="$SIMULATION_TIME"/>
    </time>
    
    <processing>
        <step-length value="1.0"/>
        <time-to-teleport value="300"/>
        <max-depart-delay value="900"/>
    </processing>
    
    <report>
        <verbose value="true"/>
        <no-step-log value="false"/>
    </report>
</configuration>
EOF

echo "Configuration file created"

