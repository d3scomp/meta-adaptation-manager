# meta-adaptation-manager
Meta-Adaptation Manager library contains a general implementation to govern meta-adaptations over any provided self-adaptive system.

## Requirements
To test this library you need to do the following:
TODO

## Implemented meta-adaptations
1. Component Isolation
Allows automatic isolation of a component by removing its ports that allows the component to interact with other components in the system. The component is isolated based on the indicated faulty knowledge it manages.
2. Collaborative Sensing
Equipes the system with the ability of knowledge injection in a situation that some component fails to produce its knowledge and there is found a correlation in the system between the failed knowledge and other relevant knowledge. This way the failed knowledge can be replaced with the closes guess from components with correlated knowledge.
3. Enhanced Mode Switching
This meta-adaptation systematically adjust the mode switching mechanism and measures its impact on the system. If there is found mode chart that improves the system performance, the system is changed accordingly.
4. Adjusting Mode Switching Parameters
This tunes the parameters of transition guards in mode switching mechanism and tries to improve the system performance similarly to the previous meta-adaptatiton.
