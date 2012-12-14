include Java


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


module JavaIO
  include_package "java.io"
end

class EngineConfig
  attr_accessor :home, :command, :bindir, :version, :arch
  @home = nil
  @bindir = nil
  @command = nil
  @version = nil
  @arch = nil

  def to_s
    "Matlab (#{@version}) : #{@home} #{@bindir} #{@command} #{@arch} bits"
  end

  def ==(another_conf)
    return self.home == another_conf.home && self.bindir == another_conf.bindir &&
        self.command == another_conf.command && self.version == another_conf.version &&
        self.arch == another_conf.arch
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
    nodeName = System.getProperty("node.name")
    if nodeName == nil
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
    @version_arch = nil

    # initialize conf file path

    @confFiles = Array.new
    @confFiles << JavaIO::File.new(tmpPath, "MatlabWorkerConfiguration.xml").getCanonicalFile()
    if schedPath != nil
      @confFiles << JavaIO::File.new(schedPath, "addons/ScilabWorkerConfiguration.xml").getCanonicalFile()
    end
    @confRead = false
  end

  def parseParams()

    puts "#{Time.new()} : Finding Matlab on #@hostname"
    if (defined?($args) && $args != nil)
      cpt = 0
      while cpt < $args.size && cpt < 100
        case $args[cpt]
          when "forceSearch"
            @forceSearch = ($args[cpt+1] == "true")
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
          when "versionArch"
            @version_arch = $args[cpt+1]
        end
        cpt += 2
      end
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

      puts "Deciding #{conf}"

      test1 = @versions_rejected != nil && @versions_rejected.index(conf.version) != nil
      test2 = @min_version != nil && inf(conf.version, @min_version)
      test3 = @max_version != nil && inf(@max_version, conf.version)
      test0 = @version_arch != nil && @version_arch.casecmp("any") != 0 && @version_arch != conf.arch
      if test1
        puts "#{conf.version} in rejected list"
      elsif test0
        puts "#{conf.version}(#{conf.arch}) don't match required arch #{@version_arch}"
      elsif test2
        puts "#{conf.version} too low"
      elsif test3
        puts "#{conf.version} too high"
      elsif conf.version == @version_pref
        puts "#{conf.version} preferred"
        return true
      else
        puts "#{conf.version} accepted"
        return true
      end
    }
    return false
  end

  # read all the configurations from the XML file
  def readConfigs()
    @confFiles.each do |confFile|
      if confFile.exists()

        puts "Reading config in #{confFile}"

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
          fisconf.close()
        end
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
        raise "In " + confFile.toString() + ", version element must match XX.xx.xx, received : " + conf.version
      end
      h = install.getChild("home")
      if h == nil
        raise "In " + confFile.toString() + ", home element must not be empty"
      end
      conf.home = h.getTextTrim()
      if conf.home.length() == 0
        raise "In " + confFile.toString() + ", home element must not be empty"
      end

      b = install.getChild("bindir")
      if b == nil
        raise "In " + confFile.toString() + ", bindir element must not be empty"
      end
      conf.bindir = b.getTextTrim()
      if conf.bindir.length() == 0
        raise "In " + confFile.toString() + ", bindir element must not be empty"
      end

      c = install.getChild("command")
      if c == nil
        raise "In " + confFile.toString() + ", command element must not be empty"
      end
      conf.command = c.getTextTrim()
      if conf.command.length() == 0
        raise "In " + confFile.toString() + ", command element must not be empty"
      end

      a = install.getChild("arch")
      if a == nil
        raise "In " + confFile.toString() + ", arch element must not be empty"
      end
      conf.arch = a.getTextTrim()
      if conf.arch.length() == 0
        raise "In " + confFile.toString() + ", arch element must not be empty"
      end


      puts "Found  #{conf}"

      @configs.push conf
    end
  end

end

$selected = false
mf = MatSciFinder.new
begin
  mf.parseParams()
  conffound = mf.readConfigs()
  if (conffound)
    $selected = mf.chooseConfig()
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
