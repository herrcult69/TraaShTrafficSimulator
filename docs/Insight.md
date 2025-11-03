# SUMO Network and Traffic Generation Guide

This guide covers creating random road networks, generating traffic, and understanding SUMO file formats for the TraaShSimulation project.

---

## Table of Contents
1. [Network Generation](#network-generation)
2. [Traffic/Route Generation](#traffic-route-generation)
3. [SUMO File Formats Explained](#sumo-file-formats-explained)
4. [Complete Workflow Example](#complete-workflow-example)

---

## Network Generation

### Using `netgenerate`

Creates road networks programmatically without manual editing.

#### Basic Grid Network
```bash
netgenerate --grid \
    --grid.number=5 \
    --grid.length=200 \
    --output-file=resource/network.net.xml
```

#### Grid with Traffic Lights
```bash
netgenerate --grid \
    --grid.number=7 \
    --grid.length=150 \
    --tls.set \
    --output-file=resource/network.net.xml
```

### Common `netgenerate` Arguments

#### Grid Network Options
| Argument | Description | Example Values |
|----------|-------------|----------------|
| `--grid` | Create grid network | (flag) |
| `--grid.number` | Number of streets in x and y direction | `5`, `7`, `10` |
| `--grid.length` | Length of streets (meters) | `100`, `200`, `500` |
| `--grid.attach-length` | Length of attached streets | `50`, `100` |

#### Spider/Radial Network Options
| Argument | Description | Example Values |
|----------|-------------|----------------|
| `--spider` | Create spider/radial network | (flag) |
| `--spider.arm-number` | Number of arms | `4`, `6`, `8` |
| `--spider.circle-number` | Number of circles | `3`, `5` |
| `--spider.space-radius` | Distance between circles (m) | `100`, `150` |

#### Random Network Options
| Argument | Description | Example Values |
|----------|-------------|----------------|
| `--rand` | Create random network | (flag) |
| `--rand.iterations` | Number of iterations | `100`, `500`, `1000` |
| `--rand.max-distance` | Maximum edge length | `200`, `500` |
| `--rand.min-distance` | Minimum edge length | `50`, `100` |

#### Traffic Light Options
| Argument | Description | Effect |
|----------|-------------|--------|
| `--tls.set` | Add traffic lights to ALL junctions | Forces TLS everywhere |
| `--tls.guess` | Add TLS to junctions with 3+ incoming edges | Smart placement |
| `--tls.join` | Join nearby traffic lights | Groups close TLS |

#### General Options
| Argument | Description | Example Values |
|----------|-------------|----------------|
| `--output-file` or `-o` | Output network file | `resource/network.net.xml` |
| `--default.speed` | Default edge speed (m/s) | `13.89` (50 km/h) |
| `--default.priority` | Default edge priority | `1`, `5`, `10` |

### Example Commands

#### Small Test Network (5x5 grid with traffic lights)
```bash
netgenerate --grid \
    --grid.number=5 \
    --grid.length=200 \
    --tls.set \
    --default.speed=13.89 \
    --output-file=resource/test_network.net.xml
```

#### Large Urban Network (10x10 grid, smart traffic lights)
```bash
netgenerate --grid \
    --grid.number=10 \
    --grid.length=150 \
    --tls.guess \
    --default.speed=11.11 \
    --output-file=resource/urban_network.net.xml
```

#### Radial/Spider Network (6 arms, 4 circles)
```bash
netgenerate --spider \
    --spider.arm-number=6 \
    --spider.circle-number=4 \
    --spider.space-radius=120 \
    --tls.set \
    --output-file=resource/spider_network.net.xml
```

#### Random Network
```bash
netgenerate --rand \
    --rand.iterations=500 \
    --rand.max-distance=300 \
    --rand.min-distance=100 \
    --output-file=resource/random_network.net.xml
```

---

## Traffic Route Generation

### Using `randomTrips.py`

Generates vehicle trips/routes for your network.

#### Basic Command
```bash
python $SUMO_HOME/tools/randomTrips.py \
    -n resource/network.net.xml \
    -r resource/routes.rou.xml \
    -e 3600
```

### `randomTrips.py` Arguments

#### Essential Arguments
| Argument | Description | Example Values |
|----------|-------------|----------------|
| `-n` or `--net-file` | Input network file | `resource/network.net.xml` |
| `-r` or `--route-file` | Output route file | `resource/routes.rou.xml` |
| `-o` or `--trip-file` | Output trip file (alternative) | `resource/trips.trips.xml` |
| `-e` or `--end` | End time (seconds) | `3600`, `7200` |

#### Traffic Density Control
| Argument | Description | Example Values | Effect |
|----------|-------------|----------------|--------|
| `--period` | Seconds between vehicles | `1`, `2`, `5`, `10` | Lower = denser traffic |
| `--insertion-rate` | Vehicles per second | `0.5`, `1.0`, `2.0` | Alternative to period |
| `-p` | Probability for vehicle | `0.1` to `1.0` | Random insertion |

#### Time Control
| Argument | Description | Example Values |
|----------|-------------|----------------|
| `--begin` or `-b` | Start time | `0`, `100` |
| `--end` or `-e` | End time | `3600`, `7200` |

#### Route Characteristics
| Argument | Description | Example Values | Effect |
|----------|-------------|----------------|--------|
| `--min-distance` | Min trip length (m) | `100`, `300`, `500` | Filters short trips |
| `--max-distance` | Max trip length (m) | `1000`, `2000`, `5000` | Filters long trips |
| `--fringe-factor` | Edge preference | `1` to `20` | Higher = more edge trips |
| `--binomial` | Binomial distribution | `3`, `5`, `10` | More realistic distances |

#### Vehicle Behavior
| Argument | Description | Example Values |
|----------|-------------|----------------|
| `--trip-attributes` | Vehicle departure attributes | See table below |
| `--vehicle-class` | Vehicle type | `passenger`, `truck`, `bus` |
| `--vclass` | Vehicle class for routing | `passenger`, `truck`, `bus` |
| `--prefix` | Vehicle ID prefix | `"car"`, `"veh"`, `"truck"` |

#### Trip Attributes (used with `--trip-attributes`)
| Attribute | Description | Example Values |
|-----------|-------------|----------------|
| `departLane` | Starting lane | `"best"`, `"random"`, `"0"`, `"1"` |
| `departSpeed` | Starting speed | `"max"`, `"random"`, `"10"` |
| `departPos` | Starting position on edge | `"random"`, `"base"`, `"last"` |
| `arrivalLane` | Arrival lane | `"current"`, `"random"` |
| `arrivalSpeed` | Arrival speed | `"current"`, `"max"` |

#### Other Useful Options
| Argument | Description | Effect |
|----------|-------------|--------|
| `--validate` | Validate routes | Checks if routes work |
| `--random` | Random routing | Less predictable |
| `--intermediate` | Add intermediate edges | More complex routes |
| `--seed` | Random seed | Reproducible results |

### Traffic Generation Examples

#### Dense City Traffic (Heavy)
```bash
python $SUMO_HOME/tools/randomTrips.py \
    -n resource/network.net.xml \
    -r resource/routes.rou.xml \
    -e 3600 \
    --period 1 \
    --fringe-factor 2 \
    --min-distance 200 \
    --max-distance 1500 \
    --trip-attributes="departLane=\"best\" departSpeed=\"random\" departPos=\"random\"" \
    --validate
```

#### Moderate Traffic
```bash
python $SUMO_HOME/tools/randomTrips.py \
    -n resource/network.net.xml \
    -r resource/routes.rou.xml \
    -e 3600 \
    --period 3 \
    --fringe-factor 10 \
    --min-distance 300 \
    --binomial 5 \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\"" \
    --prefix veh
```

#### Light Traffic (Sparse)
```bash
python $SUMO_HOME/tools/randomTrips.py \
    -n resource/network.net.xml \
    -r resource/routes.rou.xml \
    -e 3600 \
    --period 10 \
    --min-distance 500 \
    --trip-attributes="departSpeed=\"max\""
```

#### Highway Traffic (Long Distance, Fast)
```bash
python $SUMO_HOME/tools/randomTrips.py \
    -n resource/network.net.xml \
    -r resource/routes.rou.xml \
    -e 3600 \
    --period 2 \
    --fringe-factor 20 \
    --min-distance 1000 \
    --max-distance 5000 \
    --trip-attributes="departSpeed=\"max\" departLane=\"best\""
```

#### Mixed Traffic (Cars + Trucks)
```bash
# Generate car traffic
python $SUMO_HOME/tools/randomTrips.py \
    -n resource/network.net.xml \
    -r resource/cars.rou.xml \
    -e 3600 \
    --period 2 \
    --vehicle-class passenger \
    --prefix car

# Generate truck traffic
python $SUMO_HOME/tools/randomTrips.py \
    -n resource/network.net.xml \
    -r resource/trucks.rou.xml \
    -e 3600 \
    --period 10 \
    --vehicle-class truck \
    --prefix truck
```

---

## SUMO File Formats Explained

### 1. Network File (`.net.xml`)

**Purpose:** Defines the road network structure (roads, junctions, traffic lights)

**Location:** `resource/network.net.xml`

**Key Elements:**
- `<edge>` - Road segments connecting junctions
- `<junction>` - Intersections/nodes where edges meet
- `<connection>` - Defines how lanes connect through junctions
- `<tlLogic>` - Traffic light programs with phases

**Example Structure:**
```xml
<net>
    <edge id="1to2" from="1" to="2">
        <lane id="1to2_0" index="0" speed="13.89" length="200.00"/>
    </edge>
    
    <junction id="2" type="traffic_light" x="200.0" y="0.0">
        <incLanes>1to2_0 3to2_0</incLanes>
        <intLanes>:2_0_0 :2_1_0</intLanes>
    </junction>
    
    <tlLogic id="2" type="static" programID="0">
        <phase duration="31" state="GrGr"/>
        <phase duration="6"  state="yryr"/>
        <phase duration="31" state="rGrG"/>
        <phase duration="6"  state="ryry"/>
    </tlLogic>
</net>
```

**When to use:**
- Generated by `netgenerate` or created in `netedit`
- Referenced by all other SUMO files
- Can be edited manually or with netedit GUI

---

### 2. Route File (`.rou.xml`)

**Purpose:** Defines vehicle routes and traffic demand

**Location:** `resource/routes.rou.xml`

**Key Elements:**
- `<vType>` - Vehicle type definitions (length, accel, decel, color)
- `<vehicle>` - Individual vehicles with specific routes
- `<route>` - Sequence of edges the vehicle follows
- `<trip>` - Origin-destination pair (SUMO computes route)
- `<flow>` - Continuous stream of vehicles

**Example Structure:**
```xml
<routes>
    <!-- Vehicle Type Definition -->
    <vType id="car" accel="2.6" decel="4.5" sigma="0.5" length="5" maxSpeed="50"/>
    
    <!-- Single Vehicle with Explicit Route -->
    <vehicle id="veh0" type="car" depart="0.00">
        <route edges="edge1 edge2 edge3 edge4"/>
    </vehicle>
    
    <!-- Trip (SUMO computes route) -->
    <trip id="veh1" type="car" depart="10.00" from="edge1" to="edge10"/>
    
    <!-- Flow (Multiple vehicles) -->
    <flow id="flow0" type="car" begin="0" end="3600" number="100">
        <route edges="edge1 edge2 edge3"/>
    </flow>
</routes>
```

**When to use:**
- Generated by `randomTrips.py` or `duarouter`
- Can define individual vehicles or flows
- Required by simulation

---

### 3. Trip File (`.trips.xml`)

**Purpose:** Defines origin-destination pairs (simpler than routes)

**Location:** `resource/trips.trips.xml`

**Key Difference from Routes:**
- Routes: Specify exact edge sequence
- Trips: Only specify start and end, SUMO computes path

**Example Structure:**
```xml
<trips>
    <trip id="trip0" depart="0.00" from="edge1" to="edge20"/>
    <trip id="trip1" depart="5.00" from="edge5" to="edge15"/>
</trips>
```

**Converting Trips to Routes:**
```bash
duarouter -n resource/network.net.xml \
          -t resource/trips.trips.xml \
          -o resource/routes.rou.xml
```

**When to use:**
- When you don't care about exact path
- Let SUMO find optimal route
- More flexible for dynamic routing

---

### 4. Configuration File (`.sumocfg`)

**Purpose:** Master file that references all other files and sets simulation parameters

**Location:** `resource/simulation.sumocfg`

**Key Sections:**
- `<input>` - Input files (network, routes, additional)
- `<time>` - Simulation time range
- `<processing>` - Simulation behavior
- `<output>` - Output files for results
- `<gui_only>` - GUI-specific settings

**Full Example:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Input Files -->
    <input>
        <net-file value="network.net.xml"/>
        <route-files value="routes.rou.xml"/>
        <additional-files value="detectors.add.xml"/>
    </input>
    
    <!-- Simulation Time -->
    <time>
        <begin value="0"/>
        <end value="3600"/>
        <step-length value="1.0"/>
    </time>
    
    <!-- Processing Options -->
    <processing>
        <time-to-teleport value="300"/>
        <max-depart-delay value="900"/>
        <routing-algorithm value="dijkstra"/>
    </processing>
    
    <!-- Output Files -->
    <output>
        <summary-output value="summary.xml"/>
        <tripinfo-output value="tripinfo.xml"/>
        <fcd-output value="fcd.xml"/>
    </output>
    
    <!-- Reporting -->
    <report>
        <verbose value="true"/>
        <no-step-log value="false"/>
    </report>
</configuration>
```

**Common Configuration Options:**

| Section | Option | Description | Example |
|---------|--------|-------------|---------|
| `<time>` | `begin` | Simulation start time | `0` |
| `<time>` | `end` | Simulation end time | `3600` |
| `<time>` | `step-length` | Simulation step size (seconds) | `1.0`, `0.1` |
| `<processing>` | `time-to-teleport` | Seconds before teleporting stuck vehicles | `300`, `-1` (disable) |
| `<processing>` | `max-depart-delay` | Max wait time for vehicle insertion | `900` |
| `<output>` | `summary-output` | Aggregated statistics | `summary.xml` |
| `<output>` | `tripinfo-output` | Individual trip information | `tripinfo.xml` |
| `<output>` | `fcd-output` | Floating car data (positions) | `fcd.xml` |

**When to use:**
- Required to run SUMO/sumo-gui
- Central reference for all simulation files
- Easier than command-line arguments

---

### 5. Additional Files (`.add.xml`)

**Purpose:** Define detectors, traffic lights, parking, charging stations, etc.

**Location:** `resource/detectors.add.xml` (example)

**Example (Induction Loops):**
```xml
<additional>
    <inductionLoop id="detector0" lane="edge1_0" pos="50" freq="60" file="detector0.xml"/>
    <inductionLoop id="detector1" lane="edge2_0" pos="50" freq="60" file="detector1.xml"/>
</additional>
```

**When to use:**
- Add sensors for data collection
- Define custom traffic light programs
- Add bus stops, parking areas, etc.

---

## Complete Workflow Example

### Step 1: Create Network
```bash
netgenerate --grid \
    --grid.number=7 \
    --grid.length=200 \
    --tls.set \
    --default.speed=13.89 \
    --output-file=resource/network.net.xml
```

### Step 2: Generate Traffic
```bash
python $SUMO_HOME/tools/randomTrips.py \
    -n resource/network.net.xml \
    -r resource/routes.rou.xml \
    -e 3600 \
    --period 2 \
    --fringe-factor 10 \
    --min-distance 300 \
    --trip-attributes="departLane=\"best\" departSpeed=\"max\"" \
    --validate
```

### Step 3: Create/Update Configuration File
Use the existing `resource/simulation.sumocfg` or create one:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <input>
        <net-file value="network.net.xml"/>
        <route-files value="routes.rou.xml"/>
    </input>
    <time>
        <begin value="0"/>
        <end value="3600"/>
    </time>
    <processing>
        <step-length value="1.0"/>
    </processing>
</configuration>
```

### Step 4: Test with SUMO-GUI
```bash
sumo-gui -c resource/simulation.sumocfg
```

### Step 5: Run Headless (for TraaS)
```bash
sumo -c resource/simulation.sumocfg
```

### Step 6: Use in Java with TraaS
```java
SumoTraciConnection conn = new SumoTraciConnection(
    "sumo",  // or "sumo-gui"
    "resource/simulation.sumocfg"
);
conn.runServer();

for (int i = 0; i < 1000; i++) {
    conn.do_timestep();
    // Your TraaS commands here
}

conn.close();
```

---

## Quick Reference Table

| File Type | Extension | Purpose | Generated By |
|-----------|-----------|---------|--------------|
| Network | `.net.xml` | Road structure | `netgenerate`, `netedit` |
| Routes | `.rou.xml` | Vehicle routes | `randomTrips.py`, `duarouter` |
| Trips | `.trips.xml` | Origin-destination pairs | `randomTrips.py` |
| Config | `.sumocfg` | Master configuration | Manual/text editor |
| Additional | `.add.xml` | Detectors, TLS, etc. | Manual/`netedit` |

---

## Tips for Testing

1. **Start Small**: Use 5x5 grid with `--period 5` for initial tests
2. **Visualize First**: Always test with `sumo-gui` before headless
3. **Check Traffic Lights**: `grep -c "<tlLogic" resource/network.net.xml`
4. **Validate Routes**: Use `--validate` flag with `randomTrips.py`
5. **Monitor Performance**: Dense traffic + large networks = slow simulation

---

## Common Issues and Solutions

### No Vehicles Appear
- Check route file exists and has vehicles
- Verify depart times are within simulation time range
- Ensure routes are valid for the network

### Traffic Lights Not Working
- Use `--tls.set` instead of `--tls.guess`
- Check `<tlLogic>` elements exist in `.net.xml`
- Try larger network (more junctions)

### Simulation Too Slow
- Increase `--period` (fewer vehicles)
- Reduce network size
- Use `--step-length` > 1.0 (faster, less accurate)

### Vehicles Get Stuck
- Set `<time-to-teleport value="300"/>` in config
- Check for dead-ends in network
- Validate routes with `--validate`

---

## Additional Resources

- **SUMO Wiki**: https://sumo.dlr.de/docs/
- **netgenerate Docs**: https://sumo.dlr.de/docs/netgenerate.html
- **randomTrips.py Docs**: https://sumo.dlr.de/docs/Tools/Trip.html
- **TraCI/TraaS Docs**: https://sumo.dlr.de/docs/TraCI.html

---

*Generated for TraaShSimulation project - November 2, 2025*