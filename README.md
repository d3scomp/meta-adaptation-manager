# meta-adaptation-manager
Meta-Adaptation Manager library contains a general implementation to govern meta-adaptations over any provided self-adaptive system.

## Implemented meta-adaptations
1. Component Isolation
Allows automatic isolation of a component by removing its ports that allows the component to interact with other components in the system. The component is isolated based on the indicated faulty knowledge it manages.
2. Collaborative Sensing
Equipes the system with the ability of knowledge injection in a situation that some component fails to produce its knowledge and there is found a correlation in the system between the failed knowledge and other relevant knowledge. This way the failed knowledge can be replaced with the closes guess from components with correlated knowledge.
3. Enhanced Mode Switching
This meta-adaptation systematically adjust the mode switching mechanism and measures its impact on the system. If there is found mode chart that improves the system performance, the system is changed accordingly.
4. Adjusting Mode Switching Parameters
This tunes the parameters of transition guards in mode switching mechanism and tries to improve the system performance similarly to the previous meta-adaptatiton.

## Batch invocation and analysis of results

To quickly run the demo, other referenced projects are deployed and can be automatically downloaded by maven. You need to clone the "uncertain-architectures" project, switch to the JSS2017 tag and run the Simulate.py script (its description is below).

To ease the process of launching simulations with different settings and analyzing their results we have devised a set of Python scripts (version 3.5). They are placed in the analysis folder of the "uncertain-architectures" Eclipse project. The "Configuration.py" script contain the overall config for the simulation run, such as number of processor cores to use etc. The "Scenarios.py" script contains the definition of available scenarios. You can see the list by running the script without parameters. The "Simulate.py" script serves as a starting point. Run the script with the number of the selected scenario to simulate it. Once all simulation runs are finished, the "Analyze.py" script can be used to extract the values to be plotted, run the script with the number of the desired scenario. The final results are boxplots depicting the mean of the "cleaning duration" (time between a tile gets dirty until it gets clean) at each run. Create the plot by running "Plot.py" script with the scenario numbers passed as arguments (don't forget to scan the simulation results with the "Analyze.py" script first).


## Running Manually

### Requirements
Frst you need to checkout the following Github projects:
- [meta-adaptation-manager](https://github.com/d3scomp/meta-adaptation-manager) (the project featured here)
- [uncertain-architectures](https://github.com/d3scomp/uncertain-architectures.git), and switch to the "master" branch
- [JDEECo](https://github.com/d3scomp/JDEECo.git), and switch to the "uncertain-architectures" branch
- [JDEECoVisualizer](https://github.com/d3scomp/JDEECoVisualizer.git), and switch to the "uncertain-architectures" branch

Import the following Eclipse projects to a running Eclipse instance (tested with NEON.2):

- From the "meta-adaptation-manager" Github project
  - "meta-adaptation-manager"
- From the "uncertain-architectures" Github project
  - "uncertain-architectures"
- From "JDEECo" Github project
  - "cz.cuni.mff.d3s.jdeeco.core"
  - "cz.cuni.mff.d3s.jdeeco.adaptation"
  - "cz.cuni.mff.d3s.jdeeco.modes"
  - "cz.cuni.mff.d3s.jdeeco.network"
- From "JDEECoVisualizer" Github project
  - "JDEECoVisualizer"

Run maven update on all the projects (you need to install the m2e plugin to use maven within Eclipse).

### Running the demo

Locate the files Configuration and Run inside the "cz.cuni.mff.d3s.jdeeco.ua.demo" package of the "uncertain-architectures" Eclipse project. Configuration contains the parameters of the simulation and Run contains the main() of the demo.

### Visualization

To visualize a completed run, locate the "cz.filipekt.jdcv.Visualizer" class in the "JDEECoVisualizer" Eclipse project and run it. Click on Scenes->Import Scene, and on "Specify Configuration File" point to the "config.txt" generated in your file system at the "logs/runtime" folder of the "uncertain-architectures" Eclipse project. Click "Load!" and then "OK". You should be able now to see the robots moving around, cleaning, and charging. You can pause/resume the visualization using the buttons on the bottom of the window.
