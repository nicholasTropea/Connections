# Build & Execution Instructions

## Prerequisites

- **Java:** JDK 21 or later (for compilation and execution)
- **Maven:** 3.6.0 or later (for building JARs)
- **Git:** (optional, for version control)

## Quick Start

### 1. Build Everything
```bash
cd connections
mvn clean package
```

### 2. Run Server
```bash
java -jar target/connections-server.jar
```

### 3. Run Client (in another terminal)
```bash
java -jar target/connections-client.jar
```

---

## Detailed Build Steps

### Step 1: Compile the Project
```bash
mvn clean compile
```
This downloads dependencies (Gson 2.11.0, JUnit 5.10.3) and compiles all Java source files to `target/classes/`.

**Expected Output:**
```
[INFO] BUILD SUCCESS
```

### Step 2: Build Executable JARs

#### Option A: Build Both JARs Together
```bash
mvn clean package -Pbuild-server-jar -Pbuild-client-jar
```

#### Option B: Build Server JAR Only
```bash
mvn clean package -Pbuild-server-jar
# Output: target/connections-server.jar
```

#### Option C: Build Client JAR Only
```bash
mvn clean package -Pbuild-client-jar
# Output: target/connections-client.jar
```

**What These Commands Do:**
- `clean`: Removes previous build artifacts
- `package`: Compiles code and assembles JAR files
- `-Pbuild-server-jar`: Activates profile that creates executable server JAR with all dependencies
- `-Pbuild-client-jar`: Activates profile that creates executable client JAR with all dependencies

### Step 3: Run Unit Tests (Optional)
```bash
mvn test
```

**Expected Output:**
```
[INFO] Tests run: 1, Failures: 0, Errors: 0
[INFO] BUILD SUCCESS
```

---

## Direct Javac Compilation (Alternative Method)

If you prefer to compile with `javac` directly instead of Maven:

### 1. Download Gson JAR
```bash
# Create lib directory
mkdir -p lib

# Download Gson 2.11.0 to lib/
wget https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar -O lib/gson-2.11.0.jar
```

### 2. Compile All Java Files
```bash
# Set classpath with Gson
CLASSPATH="lib/gson-2.11.0.jar:src/main/resources"

# Compile server classes
javac -cp $CLASSPATH -d target/classes \
  src/main/java/com/nicholasTropea/game/model/*.java \
  src/main/java/com/nicholasTropea/game/config/*.java \
  src/main/java/com/nicholasTropea/game/net/*.java \
  src/main/java/com/nicholasTropea/game/net/requests/*.java \
  src/main/java/com/nicholasTropea/game/net/responses/*.java \
  src/main/java/com/nicholasTropea/game/server/*.java \
  src/main/java/com/nicholasTropea/game/client/*.java

# Check for errors
echo "Compilation complete. No errors if output is empty above."
```

### 3. Create Manifest Files
```bash
# Create manifest for server
mkdir -p build/META-INF
cat > build/META-INF/MANIFEST.MF << 'EOF'
Manifest-Version: 1.0
Main-Class: com.nicholasTropea.game.server.ServerMain
EOF

# Create manifest for client
cat > build/META-INF/MANIFEST_CLIENT.MF << 'EOF'
Manifest-Version: 1.0
Main-Class: com.nicholasTropea.game.client.ClientMain
EOF
```

### 4. Create JAR with javac

#### Server JAR
```bash
cd target/classes
jar cfm ../../connections-server.jar ../../build/META-INF/MANIFEST.MF \
  com/ \
  $(find . -name "*.properties" -o -name "*.json" | grep -E "\.(properties|json)$")
cd ../../
```

#### Client JAR
```bash
cd target/classes
jar cfm ../../connections-client.jar ../../build/META-INF/MANIFEST_CLIENT.MF \
  com/ \
  $(find . -name "*.properties" -o -name "*.json" | grep -E "\.(properties|json)$")
cd ../../
```

---

## Running the Application

### Via Executable JARs

#### Start Server (Terminal 1)
```bash
java -jar target/connections-server.jar
```

**Expected Output:**
```
Server started on port 5555
Listening for connections...
```

#### Start Client (Terminal 2)
```bash
java -jar target/connections-client.jar
```

**Expected Output:**
```
[Client connected to localhost:5555]
Welcome to Connections!
> 
```

### Via Maven Profiles (Interactive Development)

#### Terminal 1: Run Server
```bash
mvn -Pserver exec:java
```

#### Terminal 2: Run Client
```bash
mvn -Pclient exec:java
```

---

## Configuration

### Server Configuration
Edit `src/main/resources/config/server.properties`:

```properties
serverPort=5555                    # Port server listens on
roundDurationMilliseconds=600000   # 10 minutes per round
sessionAutosaveSeconds=15          # Auto-save every 15 seconds
gamesFilePath=src/main/resources/data/games.json
playersFilePath=src/main/resources/data/players.json
gameStatesFilePath=src/main/resources/data/gameStates.json
gameRoundStateFilePath=src/main/resources/data/gameRoundState.json
```

### Client Configuration
Edit `src/main/resources/config/client.properties`:

```properties
serverIP=localhost                 # Server address
serverPort=5555                    # Server port
clientUDPPort=0                    # UDP port (0 = auto-assign)
```

**Note:** These are read automatically at startup. No interactive prompts or CLI arguments required.

---

## Troubleshooting

### Issue: "Class not found" during compilation
**Solution:** Ensure Gson JAR is in the classpath:
```bash
# Check if Gson is in Maven cache
ls ~/.m2/repository/com/google/code/gson/gson/2.11.0/
```

### Issue: "Port already in use" when starting server
**Solution:** Change port in `server.properties`:
```properties
serverPort=9999  # Use different port
```

### Issue: "Connection refused" when starting client
**Solution:** Ensure server is running:
1. Check server is listening: `netstat -tuln | grep 5555`
2. Check config: `cat src/main/resources/config/client.properties`
3. Verify host/port match between server and client config

### Issue: JAR file runs but quits immediately
**Solution:** Check logs and ensure config files are in classpath:
```bash
# JAR must be run from project root so config/ and data/ are accessible
cd /path/to/connections
java -jar target/connections-server.jar
```

---

## File Structure for Building

```
connections/
├── pom.xml                    # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/             # All .java source files
│   │   └── resources/
│   │       ├── config/       # server.properties, client.properties
│   │       └── data/         # games.json, players.json, etc.
│   └── test/
│       └── java/             # Unit tests
├── target/
│   ├── classes/              # Compiled .class files
│   ├── connections-server.jar
│   └── connections-client.jar
└── lib/ (if using javac)
    └── gson-2.11.0.jar
```

---

## Maven Commands Reference

| Command | Purpose |
|---------|---------|
| `mvn clean` | Remove build artifacts |
| `mvn compile` | Compile source code only |
| `mvn test` | Run unit tests |
| `mvn package` | Create JARs |
| `mvn clean package` | Clean + package (recommended full build) |
| `mvn -Pserver exec:java` | Run server (development) |
| `mvn -Pclient exec:java` | Run client (development) |
| `mvn -Pbuild-server-jar package` | Build server JAR only |
| `mvn -Pbuild-client-jar package` | Build client JAR only |
| `mvn help:active-profiles` | Show active profiles |

---

## Deployment

### For Submission
1. Include entire `src/` directory
2. Include `pom.xml`
3. Include `target/connections-server.jar` and `target/connections-client.jar`
4. Include all documentation files
5. Do NOT include:
   - `.idea/` or IDE-specific files
   - `target/classes/` (not needed, only JARs)
   - `lib/` directory (if created)
   - `.git/` or version control files

### For End Users
Distribute:
1. `connections-server.jar`
2. `connections-client.jar`
3. `config/server.properties` (server only)
4. `config/client.properties` (client only)
5. `README.md` with usage instructions

Users can then:
```bash
java -jar connections-server.jar     # Terminal 1
java -jar connections-client.jar     # Terminal 2
```

---

## Verification Checklist

- [ ] `mvn clean compile` succeeds with no errors
- [ ] `mvn clean package` completes successfully
- [ ] `target/connections-server.jar` exists and is > 500 KB
- [ ] `target/connections-client.jar` exists and is > 500 KB
- [ ] `java -jar target/connections-server.jar` starts without errors
- [ ] Client can connect to server
- [ ] `mvn test` passes all tests
- [ ] No IDE files present (no `.idea/`, `.classpath`, etc.)
- [ ] All config files present and readable
- [ ] All source files have Javadoc comments

---

*Build last verified: March 18, 2026*  
*Java 21, Maven 3.6+, Gson 2.11.0*
