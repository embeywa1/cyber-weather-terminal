# Cyber Weather Information Application

A Java GUI application for retrieving real-time weather information and short-term forecasts across international cities and raw coordinates. Built for the CS1103 assignment using Java Swing and the Open-Meteo REST API.

## Features
- **Keyless REST API Integration:** Fetches live weather metrics and 5-day forecasts via Open-Meteo.
- **Location & Coordinate Search:** Search by city name (e.g., "Nairobi", "Tokyo") or raw coordinates (`-1.2864, 36.8172`).
- **Dynamic Backgrounds:** Automatically shifts color gradients based on local time (Day vs. Night) and current weather conditions.
- **Unit Conversions:** One-click toggle between Celsius (°C / km/h) and Fahrenheit (°F / mph).
- **Search History:** Tracks recent user queries with exact timestamps.
- **Asynchronous Execution:** Runs network calls on background threads (`SwingWorker`) to prevent GUI freezes.

## Compilation & Execution Instructions

Run these commands from your terminal inside the project directory:

```bash
# 1. Compile Java source files into bin/ directory
javac -d bin src/*.java

# 2. Launch Application
java -cp bin src.WeatherAppGUI



## 🚀 Terminal Setup & Walkthrough

Here is the exact step-by-step terminal execution guide on your computer:

### Step title="1. Open Terminal & Create Project Folder"
    Navigate to your assignment workspace and create the project structure:

    ```bash

    cd ~/assignments
    mkdir -p WeatherApp/src
    cd WeatherApp

    ```

  ### Step title="2. Create Source Files"
    Create the four source files inside `src/` using your preferred terminal text editor (`nano`, `vim`, or VS Code):
    ```bash
    nano src/WeatherData.java
    nano src/SearchHistoryManager.java
    nano src/WeatherService.java
    nano src/WeatherAppGUI.java
    nano README.md
    ```
    *(Paste the corresponding code blocks provided above into each file and save).*
  </Step>

  <Step title="3. Compile the Java Code">
    Compile the Java files into a clean output directory named `bin/`:
    ```bash
    javac -d bin src/*.java
    ```
  </Step>

  <Step title="4. Run the Weather Application">
    Execute the application main class:
    ```bash
    java -cp bin src.WeatherAppGUI
    ```
  </Step>
</Sequence>

---

<FollowUp label="Want me to generate an automated compile/run Bash script or draft the submission submission document?" query="Draft an automated compile.sh script and a submission text for this weather app assignment."/>
