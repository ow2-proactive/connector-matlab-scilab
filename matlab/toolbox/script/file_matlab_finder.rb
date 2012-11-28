include Java



#begin
  java_import java.lang.System
  java_import org.objectweb.proactive.core.UniqueID
  java_import org.jdom.input.SAXBuilder
  java_import org.jdom.Document
  java_import org.jdom.Element
  java_import org.jdom.Attribute
  java_import org.jdom.output.XMLOutputter
  java_import org.jdom.output.Format
  java_import java.lang.System
  java_import java.lang.Long
  java_import java.nio.channels.FileLock
  java_import java.net.InetAddress
  java_import java.util.Date
#rescue Exception => e
#  raise java.lang.RuntimeException.new(e.message + "\n" + e.backtrace.join("\n"))
#end


module JavaIO
  include_package "java.io"
end

class EngineConfig
  attr_accessor :home, :command, :bindir, :version
  @home = nil
  @bindir = nil
  @command = nil
  @version = nil

  def to_s
    "Matlab (#{@version}) : #{@home} #{@bindir} #{@command}"
  end

  def ==(another_conf)
    self.home == another_conf.home
    self.bindir == another_conf.bindir
    self.command == another_conf.command
    self.version == another_conf.version
  end
end

class MatSciFinder

  def inf(v1, v2)
    p1 = v1.split(/\./)
    p1.map! { |x| x.to_i() }

    p2 = v2.split(/\./)
    p2.map! { |x| x.to_i() }
    return (p1 <=> p2) < 0;
  end

  # initialize the research
  def initialize
    begin
      nodeName = System.getProperty("node.name")
    rescue
      nodeName = "DummyNode"
    end

    tmpPath = System.getProperty("java.io.tmpdir")
    schedPath = System.getProperty("proactive.home")

    @hostname = InetAddress.getLocalHost().getHostName()
    @ipaddress = InetAddress.getLocalHost().getHostAddress()

    @configs = Array.new

    # initialize log files and redirection stdout and stderr to those log files
    logFileJava = JavaIO::File.new(tmpPath, "FindMatlab"+nodeName+".log")
    @orig_stdout = $stdout
    @orig_stderr = $stderr
    foslog = JavaIO::FileOutputStream.new(logFileJava)
    @logout = JavaIO::PrintStream.new(foslog)
    @orig_jstdout = System.out
    @orig_jstderr = System.err
    $stdout.reopen(logFileJava.toString(), "a")
    $stdout.sync=true
    $stderr.reopen $stdout
    System.setOut(@logout)
    System.setErr(@logout)

    # initialize main variables
    @versions_rejected = nil
    @min_version = nil
    @max_version = nil
    @version_pref = nil

    # initialize conf file path

    @confFiles = Array.new
    @confFiles << JavaIO::File.new(tmpPath, "MatlabWorkerConfiguration.xml").getCanonicalFile()
    @confFiles << JavaIO::File.new(schedPath, "addons/MatlabWorkerConfiguration.xml").getCanonicalFile()
    @confRead = false
  end

  def parseParams()
    debug = ($args[0] == "true")
    if @debug
      puts "Finding Matlab on #@hostname"
    end
    cpt = 1
    while cpt < $args.size && cpt < 20
      case $args[cpt]
        when "versionPref"
          @version_pref = $args[cpt+1]
        when "versionMin"
          @min_version = $args[cpt+1]
        when "versionMax"
          @max_version = $args[cpt+1]
        when "versionRej"
          if $args[cpt+1] != nil && $args[cpt+1].length > 0
            @versions_rejected = $args[cpt+1].split(/;| |,/)

          end
      end
      cpt += 2
    end
  end

  # close streams
  def close()
    #@logWriter.close();
    $stdout = @orig_stdout
    $stderr = @orig_stderr
    System.setOut(@orig_jstdout)
    System.setErr(@orig_jstderr)
    @logout.close()
  end

  # decide if we found a valid configuration
  def chooseConfig()
    @configs.each { |conf|
      if @debug
        puts "Analysing #{conf}"
      end
      test1 = @versions_rejected != nil && @versions_rejected.index(conf.version) != nil
      test2 = @min_version != nil && inf(conf.version, @min_version)
      test3 = @max_version != nil && inf(@max_version, conf.version)
      if test1
        puts "#{conf.version} in rejected list"
      elsif test2
        puts "#{conf.version} too low"
      elsif test3
        puts "#{conf.version} too high"
      elsif conf.version == @version_pref
        puts "#{conf.version} preferred"
        return true
      else
        puts  "#{conf.version} accepted"
        return true
      end
    }
    return false
  end

  # read all the configurations from the XML file
  def readConfigs()
    @confFiles.each do |confFile|
      if confFile.exists()
        if @debug
          puts "Reading config in #{confFile}"
        end
        fisconf = JavaIO::FileInputStream.new(confFile)
        confFileLock = fisconf.getChannel().lock(0, Long::MAX_VALUE, true)
        begin
        sxb = SAXBuilder.new
        doc = sxb.build(confFile)
        racine = doc.getRootElement()
        machineGroups = racine.getChildren("MachineGroup")
        for i in 0..machineGroups.size()-1
          element = machineGroups.get(i)
          ip_pattern = element.getAttribute("ip")
          hn_pattern = element.getAttribute("hostname")
          if ip_pattern != nil
            if java::lang::String.new(@ipaddress).matches(ip_pattern.getValue())
              readConfig(element, confFile)
            end
          elsif hn_pattern != nil
            if java::lang::String.new(@hostname).matches(hn_pattern.getValue())
              readConfig(element, confFile)
            end
          end
        end
        ensure
          confFileLock.release()
        end
        fisconf.close()
        @confRead = true
        return true
      end

    end
    return false
  end

  # read one config of the XML file
  def readConfig(element, confFile)
    listInstalls = element.getChildren("matlab")
    for i in 0..listInstalls.size()-1
      install = listInstalls.get(i)
      conf = EngineConfig.new
      v = install.getChild("version")
      if v == nil
        raise "In " + confFile.toString() + ", version element must not be empty"
      end
      conf.version = v.getTextTrim()
      if conf.version.length() == 0
        raise "In " + confFile.toString() + ", version element must not be empty"
      end
      if !java::lang::String.new(conf.version).matches("^([1-9][\\d]*\\.)*[\\d]+$")
        raise "In " +  confFile.toString()  +        ", version element must match XX.xx.xx, received : " + conf.version
      end
      h = install.getChild("home")
      if h == nil
        raise "In " + confFile.toString() + ", home element must not be empty"
      end
      conf.home =  h.getTextTrim()
      if conf.home.length() == 0
        raise "In " + confFile.toString() + ", home element must not be empty"
      end

      b = install.getChild("bindir")
      if b == nil
        raise "In " + confFile.toString() + ", bindir element must not be empty"
      end
      conf.bindir =  b.getTextTrim()
      if conf.bindir.length() == 0
        raise "In " + confFile.toString() + ", bindir element must not be empty"
      end

      c = install.getChild("command")
      if c == nil
        raise "In " + confFile.toString() + ", command element must not be empty"
      end
      conf.command =  c.getTextTrim()
      if conf.command.length() == 0
        raise "In " + confFile.toString() + ", command element must not be empty"
      end

      if @debug
        puts "Found  #{conf}"
      end
      @configs.push conf
    end
  end

end

$selected = false
mf = MatSciFinder.new
begin
  if defined?($args)
    mf.parseParams()
    conffound = mf.readConfigs()
    if (conffound)
      $selected = mf.chooseConfig()
    end
  end
#rescue Exception => e
#  puts e.message + "\n" + e.backtrace.join("\n")
#  raise java.lang.RuntimeException.new(e.message + "\n" + e.backtrace.join("\n"))
ensure
  begin
    mf.close()
  rescue Exception => e
    puts e.message + "\n" + e.backtrace.join("\n")
    raise java.lang.RuntimeException.new(e.message + "\n" + e.backtrace.join("\n"))
  end
end
