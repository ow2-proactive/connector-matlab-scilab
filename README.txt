Thanks for your interest in ProActive Matlab/Scilab Connector.

ProActive Matlab/Scilab Connector {version}

You can find the documentation of ProActive Matlab/Scilab Connector in the docs folder.

Javadoc and updated documentation are available online: http://proactive.inria.fr


*** Quick start using the Matlab_Connector_bin or the Scilab_Connector_bin package :

* Download ProActive Scheduling Server from the ProActive website : http://proactive.inria.fr and install it preferably in a shared folder of your computation farm.

* Copy the contents of folder matsci_connector/scheduler_plugin into ProActive_Scheduler_server/addons/ folder

* After copying, if you use Matlab, edit the XML file MatlabWorkerConfiguration.xml inside the Scheduling/addons/ folder according to your local Matlab installation.
  The MachineGroup tag allows to specify a range of host for which the given configuration applies. Several configurations for several machine groups can be written in a single MatlabWorkerConfiguration.xml file, but
  this makes sense only if the scheduler is installed in a shared folder and every worker Node will use the same scheduler installation when starting and registering to the ResourceManager.
  When each machine uses a local scheduler-worker installation, then the scheduler_plugin folder must be copied into each addons folder of the scheduler_worker installation, and the MatlabWorkerConfiguration.xml file
  must be edited on each machine.


* If you use Scilab, edit the ScilabWorkerConfiguration.xml file. The same remarks apply as with Matlab.


* Read the instructions in SCHEDULING/README.txt in order to start the scheduler

* Note the url at which the scheduler is started, e.g. rmi://localhost:1099

* If you use Matlab :

   - Open Matlab and add path to the ProActive Matlab toolbox which is located on matlab_scilab_connector/matlab/toolbox, e.g. :

    addpath('matlab_scilab_connector/matlab/toolbox')

    - Connect to the scheduler by using the function PAconnect, e.g.:

        PAconnect('rmi://localhost:1099')

    - Enter your Scheduler login information, e.g. demo/demo

    - do a small execution test :

      r=PAsolve(@factorial, 1)

      val = PAwaitFor(r)

    - Read the documentation of Matlab Connector Toolbox from the Matlab help for more info on how to use the toolbox

* If you use Scilab :

    - Open Scilab and install JIMS (Java Interaction Mechanism in Scilab) from ATOMS.

    - build the ProActive Scilab toolbox located on matlab_scilab_connector/scilab/PAScheduler, e.g. :
        cd scilab_connector/toolbox
        exec builder.sce

    - load the toolbox into the Scilab environment (at each Scilab restart only the exec loader.sce command will be necessary)
        exec loader.sce

    - Connect to the scheduler by using the function PAconnect, e.g.:

            PAconnect('rmi://localhost:1099')

    - Enter your Scheduler login information, e.g. demo/demo

    - do a small execution test :

        r=PAsolve('cosh', 1)

        val = PAwaitFor(r)

    - Read the documentation of Scilab Connector Toolbox from the Scilab help for more info on how to use the toolbox



* For further information, please refers to the Matlab/Scilab Connector documentation in matsci_connector/doc folder,
along with Matlab toolbox and Scilab toolbox documentations (available from within Matlab and Scilab);


*** Compilation process when you use the Matlab/Scilab Connector src package or when you use a git version

Ant and a jdk >= 1.6 must be installed on your computer before building the project.

Matlab/Scilab Connector project depends on the ProActive Scheduling project. To build Matlab/Scilab Connector
it is necessary to copy in folder lib/scheduling all binaries produced as result of the Scheduling compilation:
i.e. contents of the 'Scheduling/dist/lib' folder into the 'matlab_scilab_connector/lib/scheduling'.

Please refer to the Readme file inside the scheduling project in order to compile Scheduling

When those binaries have been copied, move to the compile folder of the matlab_scilab_connector and execute :

	o Under Linux:
	  cd compile
	  ./build deploy.all  (check that the build script has executable permission)

	o Under Windows:
	  cd compile
	  build.bat deploy.all

Both Matlab and Scilab toolboxes along with the scheduler_plugin folder, will be available inside the "dist" folfer.

Please refer to the quick-start section above in order to run the toolboxes.

If you have any problems or questions when using ProActive Matlab/Scilab Connector,
feel free to contact us at proactive@ow2.org


*** Known bugs and issues:

Details can be found on the Matlab/Scilab Connector Jira bug-tracking system
http://bugs.activeeon.com/browse/MSC

*** Enjoy ProActive Matlab/Scilab Connector !
