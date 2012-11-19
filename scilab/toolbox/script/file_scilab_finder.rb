include Java
import java.lang.System
begin
  import org.ow2.proactive.scheduler.ext.scilab.worker.util.ScilabEngineConfig
  import org.ow2.proactive.scheduler.ext.scilab.worker.util.ScilabFinder
rescue Exception => e
  # The exit variable will be used to display the error which occured at startup inside the debug log file.
  # The node will not be selected and no error message will be thrown to the use (check the debug log for that)
  $exit = "Cannot load Scilab library from the scheduler addons directory, make sure it is properly installed. Detailed error message : \n"
  $exit = $exit + e.message + "\n" + e.backtrace.join("\n")
  #raise java.lang.RuntimeException.new("Cannot load Scilab library from the scheduler addons directory, make sure it is properly installed. Detailed error message :\n"+e.message + "\n" + e.backtrace.join("\n"))nd
end

module JavaIO
  include_package "java.io"
end

$selected = false
begin

  if defined?($args)

    if !defined?($exit)
      begin
        nodeName = ScilabEngineConfig.getNodeName()
      rescue
        nodeName = "DummyNode"
      end
    else
      nodeName = "DummyNode"
    end

    tmpPath = System.getProperty("java.io.tmpdir");

    logFileJava = JavaIO::File.new(tmpPath, "CheckScilab"+nodeName+".log");
    #logFile = File.new(logFileJava.toString(), "a");
    orig_stdout = $stdout
    orig_stderr = $stderr
    fos = JavaIO::FileOutputStream.new(logFileJava);
    logout = JavaIO::PrintStream.new(fos);
    orig_jstdout = System.out
    orig_jstderr = System.err;
    $stdout.reopen(logFileJava.toString(), "a")
    $stdout.sync=true
    $stderr.reopen $stdout
    System.setOut(logout);
    System.setErr(logout);

    debug = ($args[0] == "true")
    cpt = 1
    versionPref = nil
    versionMin = nil
    versionMax = nil
    versionRej = nil
    while cpt < $args.size
      case $args[cpt]
        when "versionPref"
          versionPref = $args[cpt+1]
        when "versionMin"
          versionMin = $args[cpt+1]
        when "versionMax"
          versionMax = $args[cpt+1]
        when "versionRej"
          versionRej = $args[cpt+1]

      end
      cpt += 2
    end

    if !defined?($exit)
      begin
        cf = ScilabFinder.getInstance().findMatSci(versionPref, versionRej, versionMin, versionMax, debug)
      rescue JavaIO::FileNotFoundException => fnfe
        puts fnfe.message
      rescue Exception => e
        puts e.message + "\n" + e.backtrace.join("\n")
      end

      if (cf == nil)
        puts "KO"
        $selected = false
      else
        puts cf
        ScilabEngineConfig.setCurrentConfiguration(cf)
        $selected = true
      end
    else
      puts $exit
    end
  end
rescue Exception => e
  puts e.message + "\n" + e.backtrace.join("\n")
  raise java.lang.RuntimeException.new(e.message + "\n" + e.backtrace.join("\n"))
ensure
  $stdout = orig_stdout
  $stderr = orig_stderr
  System.setOut(orig_jstdout)
  System.setErr(orig_jstderr)
  begin
    logout.close()
    fos.close()
  rescue
  end
end
