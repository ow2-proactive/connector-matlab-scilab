# ProActive Matlab/Scilab Connector

[![Build Status](http://jenkins.activeeon.com/buildStatus/icon?job=connector-matlab-scilab)](http://jenkins.activeeon.com/job/connector-matlab-scilab/)

ProActive Matlab/Scilab connector is part of the [ProActive Parallel Scientific Toolbox](http://activeeon.com/parallel-scientific-toolbox) that aims to accelerate application deployment and execution by distributing and parallelizing executions but also managing data transfers on Desktop machines, Clusters, Grids and Clouds.

## Building from sources

You can build from the master branch the bundles that contain the binaries for Matlab and Scilab with `gradle build`.

This will produce the following archives:

    build/distributions/
    ├── matlab_scilab_connector-XXXXX-SNAPSHOT-matlab.zip
    └── matlab_scilab_connector-XXXXX-SNAPSHOT-scilab.zip

## Quick start

1. Follow the installation instructions for [ProActive Scheduling](https://github.com/ow2-proactive/scheduling).

2. Unzip the desired distribution archive. The path to this folder will be referred as `CONNECTOR_HOME` in the following.

3. Copy the contents of folder `CONNECTOR_HOME/lib/worker` into `SCHEDULING_HOME/addons/client/` folder.

   3a. If you use Matlab, edit the XML file `MatlabWorkerConfiguration.xml` inside the `SCHEDULING_HOME/addons/client/` folder according to your local Matlab installation. The `MachineGroup` tag allows to specify a range of host for which the given configuration applies. Several configurations for several machine groups can be written in a single `MatlabWorkerConfiguration.xml` file, but this makes sense only if the scheduler is installed in a shared folder and every worker Node will use the same scheduler installation when starting and registering to the ResourceManager.
  When each machine uses a local scheduler worker installation, then the content of `CONNECTOR_HOME/lib/worker` folder must be copied into each `addons/client` folder of the scheduler worker installation, and the `MatlabWorkerConfiguration.xml` file must be edited on each machine.

   3b. If you use Scilab, edit the `ScilabWorkerConfiguration.xml` file, and do the same by considering Scilab.

4. Start a scheduler instance and note the url at which the scheduler is started, e.g. `pnp://localhost:64738`.

5. Load the connector from your scientific environment and connect to the scheduler.

   5a. If you use Matlab:

    - Open Matlab and add path to the ProActive Matlab toolbox which is located on `CONNECTOR_HOME`, e.g.:

    ```matlab
        addpath('CONNECTOR_HOME')
    ```
    - Connect to the scheduler by using the function PAconnect, e.g.:
    ```matlab
        PAconnect('pnp://localhost:64738')
    ```

    - Enter your Scheduler login information, e.g. `demo/demo`

    - Do a small execution test:
    ```matlab
        r=PAsolve(@factorial, 1)

        val = PAwaitFor(r)
    ```

   5b. If you use Scilab:

    - build the ProActive Scilab toolbox located on `CONNECTOR_HOME`, e.g.:
    ```scilab
        cd CONNECTOR_HOME
        exec builder.sce
    ```
    - load the toolbox into the Scilab environment (at each Scilab restart only the exec loader.sce command will be necessary)
    ```scilab
        exec loader.sce
    ```
    - Connect to the scheduler by using the function PAconnect, e.g.:
    ```scilab
        PAconnect('pnp://localhost:64738')
    ```
    - Enter your Scheduler login information, e.g. `demo/demo`

    - Do a small execution test:
    ```scilab
        r=PAsolve('cosh', 1)
        val = PAwaitFor(r)
    ```

For further information, please refers to the Matlab toolbox and Scilab toolbox documentations available from within Matlab and Scilab.

## Contact

If you have any problems or questions when using ProActive Matlab/Scilab Connector,
feel free to contact us at proactive@ow2.org
