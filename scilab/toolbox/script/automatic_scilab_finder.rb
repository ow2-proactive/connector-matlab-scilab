include Java

#require 'c:\users\fviale\workspace\matlab_scilab_connector\lib\scheduling\jdom.jar'

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
java_import java.lang.Runtime


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
    "Scilab (#{@version}) : #{@home} #{@bindir} #{@command} #{@arch} bits"
  end

  def ==(another_conf)
    return self.home == another_conf.home && self.bindir == another_conf.bindir &&
        self.command == another_conf.command && self.version == another_conf.version &&
        self.arch == another_conf.arch
  end

end

class MatSciFinder

  attr_accessor :forceSearch

  require 'rbconfig'


  # check if string v1 represents a version inferior to string v2
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

    # initialize logs
    logFileJava = JavaIO::File.new(tmpPath, "AutomaticFindScilab"+nodeName+".log")
    #logFile = File.new(logFileJava.toString(), "a");
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

    # initialize conf file
    @confFiles = Array.new
    @confFiles << JavaIO::File.new(tmpPath, "ScilabWorkerConfiguration.xml").getCanonicalFile()
    if schedPath != nil
      @confFiles << JavaIO::File.new(schedPath, "addons/ScilabWorkerConfiguration.xml").getCanonicalFile()
    end

    #@logWriter = JavaIO::PrintStream.new(JavaIO::BufferedOutputStream.new(JavaIO::FileOutputStream.new(logFile, true)));

  end


  def parseParams()

    puts "#{Time.new()} : Finding Scilab on #@hostname"
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
        puts "#{conf.version}(#{conf.arch}) in rejected list"
      elsif test0
        puts "#{conf.version}(#{conf.arch}) don't match required arch #{@version_arch}"
      elsif test2
        puts "#{conf.version}(#{conf.arch}) too low"
      elsif test3
        puts "#{conf.version}(#{conf.arch}) too high"
      else
        puts "#{conf.version}(#{conf.arch}) accepted"
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
        return true
      end
    end
    return false
  end

  # read one config of the XML file
  def readConfig(element, confFile)
    listInstalls = element.getChildren("scilab")
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

      if !@configs.index(conf)
        @configs.push(conf)
      end
    end
  end

  def findScilab()
    case MatSciFinder.os
      when :windows
        return findScilabWindows()
      when :macosx
        return findScilabUnix()
      when :linux
        return findScilabUnix()
      else
        raise "Unsupported os #{RbConfig::CONFIG['host_os']}"
    end
  end

  def findScilabWindowsInDir(dirtosearch, is64dir)
    answer = false

    pf = dirtosearch + File::SEPARATOR+ "scilab-*"
    Dir.glob(pf.to_s).each do |xx|
      puts "Analysing " + xx
      x = File.basename(xx).to_s
      conf = EngineConfig.new()
      conf.version = x[7..x.length-1]
      conf.home = xx.to_s.gsub("/", "\\")
      conf.bindir = scilabBinDir()
      conf.command = scilabCommandName()
      if is64dir
        conf.arch = "64"
      else
        conf.arch = "32"
      end
      answer = true
      #puts conf
      #puts @configs.index(conf)
      if @configs.index(conf) == nil
        @configs.push(conf)
      end
    end
    return answer
  end

  def findScilabWindows()

    scilab_versions_found = Array.new

    pfiles_x86 = System.getenv("ProgramFiles(x86)")
    pfiles_64 = System.getenv("ProgramW6432")
    pfiles = System.getenv("ProgramFiles")

    is64 = (pfiles_x86 != nil)
    if is64
      answer1 = findScilabWindowsInDir(pfiles_x86, false)
      answer2 = findScilabWindowsInDir(pfiles_64, true)
      answer = answer1 || answer2
    else
      answer = findScilabWindowsInDir(pfiles, false)
    end
    return answer
  end

  def findScilabUnix()
    answer = false
    locate_res = `locate -b -e -r "^scilab$" -q`
    if $?.to_i == 0
      locate_res.each_line do |line|
        answer = answer || findScilabUnixInLine(line)
      end
    end
    which_res = `which scilab 2>/dev/null`
    if $?.to_i == 0
      which_res.each_line do |line|
        answer = answer || findScilabUnixInLine(line)
      end
    end

    return answer
  end

  def findScilabUnixInLine(line)
    line = line.strip()
    answer = false

    t0 = File.exist?(line)
    t1 = File.readable?(line)
    t2 = File.executable?(line)
    t3 = !File.directory?(line)
    if t0 && t1 && t2 && t3
      # ok this is a scilab executable !
      conf = EngineConfig.new()
      scilabfullbin = readlink!(line)
      conf.home = File.dirname(File.dirname(scilabfullbin))
      conf.bindir = scilabBinDir()
      ver, arch = scilabVersion(scilabfullbin)
      conf.version = ver
      conf.command = scilabCommandName()
      conf.arch = arch
      if @configs.index(conf) == nil
        @configs.push(conf)
      end
      answer = true
    end
    return answer
  end


  def writeConfigs()
    if !@confFiles[0].exists() || @forceSearch
      fos = JavaIO::FileOutputStream.new(@confFiles[0])
      confFileLock = fos.getChannel().lock(0, Long::MAX_VALUE, false)
      exception = false
      begin
        mwc = Element.new("MatSciWorkerConfiguration")
        doc = Document.new(mwc)
        doc.setRootElement(mwc)
        mg = Element.new("MachineGroup")
        mg.setAttribute("hostname", ".*")
        mwc.addContent(mg)
        @configs.each { |conf|
          slb = Element.new("scilab")
          mg.addContent(slb)
          slb.addContent(Element.new("version").setText(conf.version))
          slb.addContent(Element.new("home").setText(conf.home))
          slb.addContent(Element.new("bindir").setText(conf.bindir))
          slb.addContent(Element.new("command").setText(conf.command))
          slb.addContent(Element.new("arch").setText(conf.arch))
        }
        xmlOutput = XMLOutputter.new()
        xmlOutput.setFormat(Format.getPrettyFormat())
        xmlOutput.output(doc, fos)
      rescue Exception => e
        exception = true
        raise e
      ensure
        confFileLock.release()
        fos.close()
        if exception
          if @confFiles[0].exists()
            File.delete(@confFiles[0].toString())
          end
        end
      end
    end
  end

  def checkConfig(scilabhome, version, binDirectory, command)
    home = JavaIO::File.new(scilabhome);
    if (!home.exists || !home.canRead || !home.isDirectory)
      puts home.toString() + " cannot be found."
      return nil;
    end
    bindir = JavaIO::File.new(home, binDirectory)
    if (!bindir.exists || !bindir.canRead || !bindir.isDirectory)
      puts bindir.toString() + " cannot be found."
      return nil;
    end
    comm = JavaIO::File.new(bindir, command)
    if (!comm.exists || !comm.canExecute || !comm.isFile)
      puts comm.toString() + " cannot be found."
      return nil;
    end
  end


  def scilabVersion(bin)
    version = `#{bin} -version`
    if $?.to_i == 256

      version.each_line do |l|

        if l["scilab-"]
          return scilabVersionAndArchClean(l)
        end
      end
    end
  end

  def scilabVersionAndArchClean(version)
    return version.gsub(/scilab-|\(64-bit\)/, '').strip(), version.index("(64-bit)") ? "64" : "32"
  end

  def scilabBinDir
    case MatSciFinder.os
      when :windows
        if arch64?
          "bin"
        else
          "bin"
        end
      when :linux
        if arch64?
          return "bin"
        else
          "bin"
        end
      when :macosx
        if arch64?
          "bin"
        else
          "bin"
        end
      else
        raise "Unsupported os #{RbConfig::CONFIG['host_os']}"
    end
  end

  def scilabCommandName
    case MatSciFinder.os
      when :windows
        if arch64?
          "Scilex.exe"
        else
          "Scilex.exe"
        end
      when :linux
        if arch64?
          return "scilab"
        else
          "scilab"
        end
      when :macosx
        if arch64?
          "scilab"
        else
          "scilab"
        end
      else
        raise "Unsupported os #{RbConfig::CONFIG['host_os']}"
    end
  end

  def MatSciFinder.os
    @os ||= begin
      case RbConfig::CONFIG['host_os']
        when /mswin|msys|mingw32|Windows/
          :windows
        when /darwin|mac os/
          :macosx
        when /linux/
          :linux
        when /solaris|bsd/
          :unix
        else
          # unlikely
          raise "unknown os #{RbConfig::CONFIG['host_os']}"
      end
    end
  end

  def readlink!(path)
    path = File.expand_path(path)
    return path unless File.symlink?(path)
    dirname = File.dirname(path)
    readlink = File.readlink(path)
    if not readlink =~ /^\// # it's a relative path
      readlink = dirname + '/'+ readlink # make it absolute
    end
    readlink = File.expand_path(readlink) # eliminate this/../../that
    if File.symlink?(readlink)
      return File.readlink!(readlink) # recursively follow symlinks
    else
      return readlink
    end
  end


  def bits
    @bits ||= (
    if 1.size == 8
      64
    else
      32
    end
    )
    #@bits ||= (
    #if %w[ x86_64 amd64 i686 ].include? RbConfig::CONFIG['host_cpu']
    #  64
    #else
    #  32
    #end
    #)
  end

  def find(* paths) # :yield: path
    paths.collect! { |d| d.dup }
    while file = paths.shift
      catch(:prune) do
        yield file.dup.taint
        next unless File.exist? file
        begin
          if File.lstat(file).directory? then
            d = Dir.open(file)
            begin
              for f in d
                next if f == "." or f == ".."
                if File::ALT_SEPARATOR and file =~ /^(?:[\/\\]|[A-Za-z]:[\/\\]?)$/ then
                  f = file + f
                elsif file == "/" then
                  f = "/" + f
                else
                  f = File.join(file, f)
                end
                paths.unshift f.untaint
              end
            ensure
              d.close
            end
          end
        rescue Errno::ENOENT, Errno::EACCES
        end
      end
    end
  end

  def prune
    throw :prune
  end

  def MatSciFinder.windows?
    os == :windows
  end

  def arch64?
    bits == 64
  end

end


$selected = false
#$args=["forceSearch","true", "versionPref", "5.4.0"]

mf = MatSciFinder.new
begin

  mf.parseParams()
  conffound = false
  if !mf.forceSearch
    conffound = mf.readConfigs()
  end
  if !conffound
    conffound = mf.findScilab()

    mf.writeConfigs()

  end
  if conffound
    $selected = mf.chooseConfig()
  else
    puts "no scilab installation found"
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
