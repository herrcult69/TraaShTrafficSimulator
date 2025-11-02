#!/bin/bash

# SUMO Network and Traffic Generation Script
# Generates a complete simulation setup with mixed vehicle types

set -e  # Exit on error

# ====================
# Configuration
# ====================
RESOURCE_DIR="resource"
NETWORK_FILE="$RESOURCE_DIR/network.net.xml"
CONFIG_FILE="$RESOURCE_DIR/simulation.sumocfg"
SIMULATION_TIME=3600  # 1 hour in seconds

# Network parameters (RANDOM NETWORK)
RAND_ITERATIONS=100      # Number of random network iterations
RAND_MAX_DISTANCE=50   # Maximum edge length in meters
RAND_MIN_DISTANCE=10    # Minimum edge length in meters
DEFAULT_SPEED=15.89      # m/s

# Traffic density (seconds between vehicles)
CAR_PERIOD=5        
TRUCK_PERIOD=15    
MOTORCYCLE_PERIOD=30 
BUS_PERIOD=10       
EMERGENCY_PERIOD=30 

# ====================
# Create resource directory
# ====================
mkdir -p "$RESOURCE_DIR"

echo "=========================================="
echo "SUMO Simulation Generator (Random Network)"
echo "=========================================="

# ====================
# Step 1: Generate Random Network
# ====================
echo ""
echo "[1/4] Generating random road network..."
netgenerate --rand \
    --rand.iterations=$RAND_ITERATIONS \
    --rand.max-distance=$RAND_MAX_DISTANCE \
    --rand.min-distance=$RAND_MIN_DISTANCE \
    --tls.guess \
    --default.speed=$DEFAULT_SPEED \
    --junctions.join \
    --output-file="$NETWORK_FILE"

if [ $? -eq 0 ]; then
    TLS_COUNT=$(grep -c "<tlLogic" "$NETWORK_FILE" || echo "0")
    EDGE_COUNT=$(grep -c "<edge id" "$NETWORK_FILE" || echo "0")
    JUNCTION_COUNT=$(grep -c "<junction id" "$NETWORK_FILE" || echo "0")
    echo "✓ Random network created:"
    echo "  Junctions: $JUNCTION_COUNT"
    echo "  Edges: $EDGE_COUNT"
    echo "  Traffic lights: $TLS_COUNT"
else
    echo "✗ Network generation failed!"
    exit 1
fi

# ====================
# Step 2: Generate Traffic for Each Vehicle Type
# ====================
echo ""
echo "[2/4] Generating traffic routes..."

# Cars
echo "  - Generating cars (period: ${CAR_PERIOD}s)..."
python3 $SUMO_HOME/tools/randomTrips.py \
    -n "$NETWORK_FILE" \
    -r "$RESOURCE_DIR/cars.rou.xml" \
    -e $SIMULATION_TIME \
    --period $CAR_PERIOD \
    --fringe-factor 10 \
    --min-distance 300 \
    --vehicle-class passenger \
    --prefix car \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\" color=\"1,0,0\"" \
    --validate 2>/dev/null

# Trucks
echo "  - Generating trucks (period: ${TRUCK_PERIOD}s)..."
python3 $SUMO_HOME/tools/randomTrips.py \
    -n "$NETWORK_FILE" \
    -r "$RESOURCE_DIR/trucks.rou.xml" \
    -e $SIMULATION_TIME \
    --period $TRUCK_PERIOD \
    --fringe-factor 15 \
    --min-distance 500 \
    --vehicle-class truck \
    --prefix truck \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\" color=\"0,0,1\"" \
    --validate 2>/dev/null

# Motorcycles
echo "  - Generating motorcycles (period: ${MOTORCYCLE_PERIOD}s)..."
python3 $SUMO_HOME/tools/randomTrips.py \
    -n "$NETWORK_FILE" \
    -r "$RESOURCE_DIR/motorcycles.rou.xml" \
    -e $SIMULATION_TIME \
    --period $MOTORCYCLE_PERIOD \
    --fringe-factor 8 \
    --min-distance 200 \
    --vehicle-class motorcycle \
    --prefix moto \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\" color=\"0,0,0\"" \
    --validate 2>/dev/null

# Buses
echo "  - Generating buses (period: ${BUS_PERIOD}s)..."
python3 $SUMO_HOME/tools/randomTrips.py \
    -n "$NETWORK_FILE" \
    -r "$RESOURCE_DIR/buses.rou.xml" \
    -e $SIMULATION_TIME \
    --period $BUS_PERIOD \
    --fringe-factor 12 \
    --min-distance 400 \
    --vehicle-class bus \
    --prefix bus \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\" color=\"0,1,0\"" \
    --validate 2>/dev/null

# Emergency vehicles
echo "  - Generating emergency vehicles (period: ${EMERGENCY_PERIOD}s)..."
python3 $SUMO_HOME/tools/randomTrips.py \
    -n "$NETWORK_FILE" \
    -r "$RESOURCE_DIR/emergency.rou.xml" \
    -e $SIMULATION_TIME \
    --period $EMERGENCY_PERIOD \
    --fringe-factor 20 \
    --min-distance 600 \
    --vehicle-class emergency \
    --prefix ambulance \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\" color=\"1,1,0\"" \
    --validate 2>/dev/null

echo "✓ Traffic routes generated"

# ====================
# Step 3: Count Generated Vehicles
# ====================
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

# ====================
# Step 4: Create Configuration File
# ====================
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

echo "✓ Configuration file created"

# ====================
# Summary
# ====================
echo ""
echo "=========================================="
echo "Simulation Setup Complete!"
echo "=========================================="
echo ""
echo "Generated files in $RESOURCE_DIR/:"
echo "  - network.net.xml       (Random network: ${JUNCTION_COUNT} junctions, ${EDGE_COUNT} edges, ${TLS_COUNT} TLS)"
echo "  - cars.rou.xml          ($CAR_COUNT vehicles)"
echo "  - trucks.rou.xml        ($TRUCK_COUNT vehicles)"
echo "  - motorcycles.rou.xml   ($MOTO_COUNT vehicles)"
echo "  - buses.rou.xml         ($BUS_COUNT vehicles)"
echo "  - emergency.rou.xml     ($EMERGENCY_COUNT vehicles)"
echo "  - simulation.sumocfg    (Main config file)"
echo ""
echo "Network parameters:"
echo "  Iterations: $RAND_ITERATIONS"
echo "  Edge length: ${RAND_MIN_DISTANCE}-${RAND_MAX_DISTANCE}m"
echo "Simulation time: ${SIMULATION_TIME}s ($(($SIMULATION_TIME / 60)) minutes)"
echo ""
echo "To run simulation:"
echo "  GUI:      sumo-gui -c $CONFIG_FILE"
echo "  Headless: sumo -c $CONFIG_FILE"
echo ""
echo "To adjust network complexity, edit these variables:"
echo "  RAND_ITERATIONS=$RAND_ITERATIONS      (higher = more complex)"
echo "  RAND_MAX_DISTANCE=$RAND_MAX_DISTANCE  (edge length)"
echo ""
echo "To adjust traffic density, edit these variables:"
echo "  CAR_PERIOD=$CAR_PERIOD        (lower = more cars)"
echo "  TRUCK_PERIOD=$TRUCK_PERIOD"
echo "  MOTORCYCLE_PERIOD=$MOTORCYCLE_PERIOD"
echo "  BUS_PERIOD=$BUS_PERIOD"
echo "  EMERGENCY_PERIOD=$EMERGENCY_PERIOD"
echo ""