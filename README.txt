Thanks for your interest in ProActive Matlab/Scilab Connector.

ProActive Matlab/Scilab Connector {version}

You can find the documentation of ProActive Matlab/Scilab Connector in the docs directory.

Javadoc and updated documentation are available online: http://proactive.inria.fr



*** Quick start :

* Download ProActive Scheduling Server from the ProActive website : http://proactive.inria.fr

* Copy matlab_scilab_connector/dist/lib/ProActive_Matlab_Scilab.jar into Scheduling/addons/ directory or run the ant target deploy.matsci.scheduling

* If you use Matlab, choose any of the files, depending on your environment :
    - matlab_scilab_connector/matlab/config/worker/MatlabConfigurationTemplateUnix.xml
    - matlab_scilab_connector/matlab/config/worker/MatlabConfigurationTemplateWindows.xml
  Edit it according to your local Matlab installation, rename it to MatlabConfiguration.xml and copy this file into
  SCHEDULING/addons/

* If you use Scilab, choose any of the files, depending on your environment :
    - matlab_scilab_connector/scilab/config/worker/ScilabConfigurationTemplateUnix.xml
    - matlab_scilab_connector/scilab/config/worker/ScilabConfigurationTemplateWindows.xml
  Edit it according to your local Scilab installation, rename it to ScilabConfiguration.xml and copy this file into
  SCHEDULING/addons/


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

    - Read the documentation of ProActive Scheduler Toolbox from the Matlab help for more info on how to use the toolbox

* If you use Scilab :

    - Open Scilab and install JIMS (Java Interaction Mechanism in Scilab) from ATOMS.

    - build the ProActive Scilab toolbox located on matlab_scilab_connector/scilab/PAScheduler, e.g. :
        cd matlab_scilab_connector/scilab/PAScheduler
        exec builder.sce

    - load the toolbox into the Scilab environment
        exec loader

    - Connect to the scheduler by using the function PAconnect, e.g.:

            PAconnect('rmi://localhost:1099')

    - Enter your Scheduler login information, e.g. demo/demo

    - do a small execution test :

        r=PAsolve('cosh', 1)

        val = PAwaitFor(r)

    - Read the documentation of ProActive Scheduler Toolbox from the Scilab help for more info on how to use the toolbox



* For further information, please refers to the Matlab/Scilab Connector documentation in matlab_scilab_connector/doc/built,
along with Matlab toolbox and Scilab toolbox documentations (available from within Matlab and Scilab);


*** Prepare project for compilation

Matlab/Scilab Connector project depends on the ProActive Scheduling project. To build Matlab/Scilab Connector
it is necessary to copy there all binaries produced as result of Scheduling
compilation: content of the 'Scheduling/dist/lib' into the 'matlab_scilab_connector/lib/scheduling'.
This can be done using special ant target (this target assumes that special build property 
'scheduling.project.dir' contains path to the compiled ProActive Scheduling project):
    o Under Linux:
      cd compile
      ./build copy.dependencies  (check that the build script has executable permission)

    o Under Windows:
      cd compile
      build.bat copy.dependencies

Also special ant script was created for quick start with Matlab/Scilab Connector project:
- check out 'build' project (git://gitorious.ow2.org/ow2-proactive/build.git)
- go to the build project
- execute 'ant prepare-matsciConnector'
This ant target will check out ProActive Programming, Scheduling and Matlab/Scilab Connector projects,
compile Programming and Scheduling and will copy all required dependencies into the Matlab/Scilab Connector,
after this Matlab/Scilab Connector project is completely ready for compilation (note: before executing
ant script it is possible to modify some script parameters like svn url to use, 
see build/build.properties for all available options).
  

*** Compilation :

If you want to recompile all sources and generate all jar files:

	o Under Linux:
	  cd compile
	  ./build deploy.all  (check that the build script has executable permission)

	o Under Windows:
	  cd compile
	  build.bat deploy.all

If you want only to compile all sources (and not the jar files):

	o Under Linux:
	  cd compile
	  ./build compile.all  (check that the build script has executable permission)

	o Under Windows:
	  cd compile
	  build.bat compile.all

If you have any problems or questions when using ProActive Matlab/Scilab Connector,
feel free to contact us at proactive@ow2.org


*** Known bugs and issues:

Details can be found on the ProActive Jira bug-tracking system
(http://bugs.activeeon.com/):

*** Enjoy ProActive Matlab/Scilab Connector !
