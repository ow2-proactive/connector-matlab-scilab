include Java


java_import javax.xml.parsers.DocumentBuilder
java_import javax.xml.parsers.DocumentBuilderFactory
java_import org.w3c.dom.Document
java_import org.w3c.dom.Element
java_import org.w3c.dom.Attr
java_import org.w3c.dom.Node
java_import org.w3c.dom.NodeList
java_import javax.xml.transform.TransformerFactory;
java_import javax.xml.transform.dom.DOMSource;
java_import javax.xml.transform.stream.StreamResult;
java_import javax.xml.transform.OutputKeys
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
    "Matlab (#{@version}) : #{@home} #{@bindir} #{@command} #{@arch} bits"
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

    begin
      nodeName = org.objectweb.proactive.api.PAActiveObject.getNode().getNodeInformation().getName()
    rescue Exception => e
      puts e.message + "\n" + e.backtrace.join("\n")
    end

    if nodeName == nil
      nodeName = "DummyNode"
    end

    tmpPath = System.getProperty("java.io.tmpdir")
    schedPath = System.getProperty("proactive.home")

    if schedPath == nil
      schedPath = System.getProperty("pa.scheduler.home")
    end
    if schedPath == nil
      schedPath = System.getProperty("pa.rm.home")
    end

    @hostname = InetAddress.getLocalHost().getHostName()
    @ipaddress = InetAddress.getLocalHost().getHostAddress()

    @configs = Array.new

    # initialize conf file
    @confFiles = Array.new
    @confFiles << JavaIO::File.new(tmpPath, "MatlabWorkerConfiguration.xml").getCanonicalFile()
    if schedPath != nil
      @confFiles << JavaIO::File.new(schedPath, "addons/client/MatlabWorkerConfiguration.xml").getCanonicalFile()
    end
    @corruptedConfigFiles = Array.new(@confFiles.length)
  end


  def parseParams()

    puts "#{Time.new()} : Finding Matlab on #@hostname"

    cpt = 0
    if (defined?(args) && args != nil)
      while cpt < args.size && cpt < 100
        case args[cpt]
          when "forceSearch"
            @forceSearch = (args[cpt+1] == "true")
          when "versionPref"
            @version_pref = args[cpt+1]
          when "versionMin"
            @min_version = args[cpt+1]
          when "versionMax"
            @max_version = args[cpt+1]
          when "versionRej"
            if args[cpt+1] != nil && args[cpt+1].length > 0
              @versions_rejected = args[cpt+1].split(/;| |,/)

            end
          when "versionArch"
            @version_arch = args[cpt+1]
        end
        cpt += 2
      end
    end
  end

  def close()

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

  def aquireLock(fisorfos, shared)
    confFileLock = nil
    nbattempts = 0
    while confFileLock == nil && nbattempts < 600
      begin
        confFileLock = fisorfos.getChannel().lock(0, Long::MAX_VALUE, shared)
      rescue Exception => e
        puts e.message + "\n" + e.backtrace.join("\n")
        sleep(1)
        puts "Error occurred while acquiring lock, retrying ..."
        nbattempts+=1
      end
    end
    return confFileLock
  end

  # read all the configurations from the XML file
  def readConfigs()
    for i in 0..@confFiles.length-1 do
      confFile = @confFiles[i]
      @corruptedConfigFiles[i] = false
      puts "Looking for config file at #{confFile}"
      if confFile.exists()

        puts "Reading config in #{confFile}"

        fisconf = JavaIO::FileInputStream.new(confFile)
        confFileLock = aquireLock(fisconf, true)
        begin
          dbFactory = DocumentBuilderFactory.newInstance()
          dBuilder = dbFactory.newDocumentBuilder()
          doc = dBuilder.parse(confFile)
          racine = doc.getDocumentElement()
          machineGroups = racine.getElementsByTagName("MachineGroup")
          for i in 0..machineGroups.getLength()-1
            element = machineGroups.item(i)
            if (element.java_kind_of?(Element))
              ip_pattern = element.getAttribute("ip")
              hn_pattern = element.getAttribute("hostname")
              if ip_pattern != nil && ip_pattern != ""
                if java::lang::String.new(@ipaddress).matches(ip_pattern)
                  readConfig(element, confFile)
                end
              elsif hn_pattern != nil && hn_pattern != ""
                if java::lang::String.new(@hostname).matches(hn_pattern)
                  readConfig(element, confFile)
                end
              end
            end
          end
        rescue Exception => e
          puts e.message + "\n" + e.backtrace.join("\n")
          @corruptedConfigFiles[i] = true
          return false
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
    listInstalls = element.getElementsByTagName("matlab")
    for i in 0..listInstalls.getLength()-1
      install = listInstalls.item(i)
      conf = EngineConfig.new
      nl = install.getElementsByTagName("version")
      if nl.getLength() != 1
        raise "In " + confFile.toString() + ", version element must not be empty"
      end
      conf.version = nl.item(0).getTextContent().strip()
      if conf.version.length() == 0
        raise "In " + confFile.toString() + ", version element must not be empty"
      end
      if !java::lang::String.new(conf.version).matches("^([1-9][\\d]*\\.)*[\\d]+$")
        raise "In " + confFile.toString() + ", version element must match XX.xx.xx, received : " + conf.version
      end
      nl = install.getElementsByTagName("home")
      if nl.getLength() != 1
        raise "In " + confFile.toString() + ", home element must not be empty"
      end
      conf.home = nl.item(0).getTextContent().strip()
      if conf.home.length() == 0
        raise "In " + confFile.toString() + ", home element must not be empty"
      end

      nl = install.getElementsByTagName("bindir")
      if nl.getLength() != 1
        raise "In " + confFile.toString() + ", bindir element must not be empty"
      end
      conf.bindir = nl.item(0).getTextContent().strip()
      if conf.bindir.length() == 0
        raise "In " + confFile.toString() + ", bindir element must not be empty"
      end

      nl = install.getElementsByTagName("command")
      if nl.getLength() != 1
        raise "In " + confFile.toString() + ", command element must not be empty"
      end
      conf.command = nl.item(0).getTextContent().strip()
      if conf.command.length() == 0
        raise "In " + confFile.toString() + ", command element must not be empty"
      end

      nl = install.getElementsByTagName("arch")
      if nl.getLength() != 1
        raise "In " + confFile.toString() + ", arch element must not be empty"
      end
      conf.arch = nl.item(0).getTextContent().strip()
      if conf.arch.length() == 0
        raise "In " + confFile.toString() + ", arch element must not be empty"
      end


      puts "Found  #{conf}"

      if !@configs.index(conf)
        @configs.push(conf)
      end
    end
  end

  def findMatlab()
    case MatSciFinder.os
      when :windows
        return findMatlabWindows()
      when :macosx
        return findMatlabMac()
      when :linux
        return findMatlabUnix()
      else
        raise "Unsupported os #{RbConfig::CONFIG['host_os']}"
    end
  end

  def findMatlabWindowsInDir(dirtosearch, is64dir)
    answer = false

    pf = dirtosearch + File::SEPARATOR + "MATLAB" + File::SEPARATOR+ "R*"
    Dir.glob(pf.to_s).each do |xx|
      puts "Analysing " + xx
      begin
        x = File.basename(xx).to_s
        conf = EngineConfig.new()
        conf.version = matlabYearToVersion(x[1..x.length-1])
        conf.home = xx.to_s.gsub("/", "\\")
        conf.bindir = matlabBinDir(is64dir)
        conf.command = matlabCommandName()
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
          puts "Added " + conf.to_s
        else
          puts "Skipped already added " + conf.to_s
        end

      rescue Exception => e
        puts e.message + "\n" + e.backtrace.join("\n")
        puts "Error occurred, skipping ..."
      end
    end
    return answer
  end

  def findMatlabWindows()

    scilab_versions_found = Array.new

    pfiles_x86 = System.getenv("ProgramFiles(x86)")
    pfiles_64 = System.getenv("ProgramW6432")
    pfiles = System.getenv("ProgramFiles")

    is64 = (pfiles_x86 != nil)
    if is64
      answer1 = findMatlabWindowsInDir(pfiles_x86, false)
      answer2 = findMatlabWindowsInDir(pfiles_64, true)
      answer = answer1 || answer2
    else
      answer = findMatlabWindowsInDir(pfiles, false)
    end
    return answer
  end

  def findMatlabUnix()
    answer = false
    locate_res = `locate -b -e -r "^MATLAB$" -q`
    if $?.to_i == 0
      locate_res.each_line do |line|
        answer = findMatlabUnixInLine(line) || answer
      end
    end
    locate_res = `locate -b -e -r "^matlab$" -q`
    if $?.to_i == 0
      locate_res.each_line do |line|
        answer = findMatlabUnixInLine(line) || answer
      end
    end
    which_res = `which matlab 2>/dev/null`
    if $?.to_i == 0
      which_res.each_line do |line|
        answer = findMatlabUnixInLine(line) || answer
      end
    end
    t = Time.now
    for i in 2006..t.year
      which_res = `which matlab#{i}a 2>/dev/null`
      if $?.to_i == 0
        which_res.each_line do |line|
          answer = findMatlabUnixInLine(line) || answer
        end
      end
      which_res = `which matlab#{i}b 2>/dev/null`
      if $?.to_i == 0
        which_res.each_line do |line|
          answer = findMatlabUnixInLine(line) || answer
        end
      end
    end

    return answer
  end

  def findMatlabMac()
    answer = false
    locate_res = `locate /Applications/*/matlab`
    if $?.to_i == 0
      locate_res.each_line do |line|
        answer = findMatlabMacInLine(line) || answer
      end
    end
    locate_res = `locate /Applications/*/MATLAB`
    if $?.to_i == 0
      locate_res.each_line do |line|
        answer = findMatlabMacInLine(line) || answer
      end
    end
    which_res = `which matlab 2>/dev/null`
    if $?.to_i == 0
      which_res.each_line do |line|
        answer = findMatlabMacInLine(line) || answer
      end
    end
    
    return answer
  end


  def findMatlabUnixInLine(line)
    line = line.strip()
    answer = false
    puts "Analysing " +line
    t0 = File.exist?(line)
    if !t0
      puts "doesn't exist"
    end
    t1 = File.readable?(line)
    if !t1
      puts "is not readable"
    end
    begin
      t2_1 = File.executable?(line)
    rescue Exception => e
      puts e.message + "\n" + e.backtrace.join("\n")
      t2_1 = false
    end
    jf = JavaIO::File.new(line)
    t2_2 = jf.canExecute()
    t2 = t2_1 || t2_2
    if !t2
      puts "cannot be executed"
    end
    t3 = !File.directory?(line)
    if !t3
      puts "is a directory"
    end
    if t0 && t1 && t2 && t3
      # ok this is a matlab executable !
      begin

        matlabfullbin = readlink!(line)
	matlabcmd = File.basename(matlabfullbin)
	if matlabcmd != "matlab" && matlabcmd != "MATLAB"
		puts "Wrong executable name : " + matlabcmd
		return false 
	end

        fullbindir = File.dirname(matlabfullbin)
        archdir = File.basename(fullbindir)
        if archdir == "bin"
          matlabhome = File.dirname(fullbindir)
          Dir.glob(fullbindir.to_s+"/glnx*").each do |xx|
            conf = EngineConfig.new()
            conf.command = "MATLAB"
            archdir = File.basename(xx)
            conf.bindir = "bin/"+ archdir
            conf.arch = (archdir == "glnxa64") ? "64" : "32"
            conf.home = matlabhome

            matlabyear = File.basename(matlabhome).to_s
            matlabyear = matlabyear[6..matlabyear.length-1]
            conf.version = matlabYearToVersion(matlabyear)
            if @configs.index(conf) == nil
              @configs.push(conf)
              puts "Added " + conf.to_s
              answer = true
            else
              puts "Skipped already added " + conf.to_s
            end
          end
        else
          conf = EngineConfig.new()
          matlabhome = File.dirname(File.dirname(fullbindir))
          conf.bindir = "bin/"+ archdir
          conf.arch = (archdir == "glnxa64") ? "64" : "32"
          conf.home = matlabhome

          matlabyear = File.basename(matlabhome).to_s
          matlabyear = matlabyear[6..matlabyear.length-1]
          conf.version = matlabYearToVersion(matlabyear)
          conf.command = "MATLAB"
          if @configs.index(conf) == nil
            @configs.push(conf)
            puts "Added " + conf.to_s
            answer = true
          else
            puts "Skipped already added " + conf.to_s
          end
        end

      rescue Exception => e
        puts e.message + "\n" + e.backtrace.join("\n")
        puts "Error occurred, skipping ..."
      end
      return answer
    end
  end

def findMatlabMacInLine(line)
    line = line.strip()
    answer = false
    puts "Analysing " +line
    t0 = File.exist?(line)
    if !t0
      puts "doesn't exist"
    end
    t1 = File.readable?(line)
    if !t1
      puts "is not readable"
    end
    t2_1 = File.executable?(line)
    jf = JavaIO::File.new(line)
    t2_2 = jf.canExecute()
    t2 = t2_1 || t2_2
    if !t2
      puts "cannot be executed"
    end
    t3 = !File.directory?(line)
    if !t3
      puts "is a directory"
    end
    if t0 && t1 && t2 && t3
      # ok this is a matlab executable !
      begin

        matlabfullbin = readlink!(line)
	matlabcmd = File.basename(matlabfullbin)
	if matlabcmd != "matlab" && matlabcmd != "MATLAB"
		puts "Wrong executable name : " + matlabcmd
		return false 
	end

        fullbindir = File.dirname(matlabfullbin)
        archdir = File.basename(fullbindir)
        if archdir == "bin"
          matlabhome = File.dirname(fullbindir)
          Dir.glob(fullbindir.to_s+"/mac*").each do |xx|
            conf = EngineConfig.new()
            conf.command = "MATLAB"
            archdir = File.basename(xx)
            conf.bindir = "bin/"+ archdir
            conf.arch = (archdir == "maci64") ? "64" : "32"
            conf.home = matlabhome

            matlabyear = File.basename(matlabhome).to_s
            matlabyear = matlabyear[8,5]
            conf.version = matlabYearToVersion(matlabyear)
            if @configs.index(conf) == nil
              @configs.push(conf)
              puts "Added " + conf.to_s
              answer = true
            else
              puts "Skipped already added " + conf.to_s
            end
          end
        else
          conf = EngineConfig.new()
          matlabhome = File.dirname(File.dirname(fullbindir))
          conf.bindir = "bin/"+ archdir
          conf.arch = (archdir == "maci64") ? "64" : "32"
          conf.home = matlabhome

          matlabyear = File.basename(matlabhome).to_s
          matlabyear = matlabyear[8,5]
          conf.version = matlabYearToVersion(matlabyear)
          conf.command = "MATLAB"
          if @configs.index(conf) == nil
            @configs.push(conf)
            puts "Added " + conf.to_s
            answer = true
          else
            puts "Skipped already added " + conf.to_s
          end
        end

      rescue Exception => e
        puts e.message + "\n" + e.backtrace.join("\n")
        puts "Error occurred, skipping ..."
      end
      return answer
    end
  end



  def writeConfigs()
    if !@confFiles[0].exists() || @forceSearch || @corruptedConfigFiles[0]
      if !@confFiles[0].exists()
        @confFiles[0].createNewFile()
        @confFiles[0].setReadable(true, false)
        @confFiles[0].setWritable(true, false)
      end

      fos = JavaIO::FileOutputStream.new(@confFiles[0])
      confFileLock = aquireLock(fos, false)
      exception = false
      begin
        dbFactory = DocumentBuilderFactory.newInstance()
        dBuilder = dbFactory.newDocumentBuilder()
        doc = dBuilder.newDocument()

        mwc = doc.createElement("MatSciWorkerConfiguration")
        doc.appendChild(mwc)
        mg = doc.createElement("MachineGroup")
        mg.setAttribute("hostname", ".*")
        mwc.appendChild(mg)

        @configs.each { |conf|
          slb = doc.createElement("matlab")
          mg.appendChild(slb)
          ver = doc.createElement("version")
          ver.appendChild(doc.createTextNode(conf.version))
          slb.appendChild(ver)
          hom = doc.createElement("home")
          hom.appendChild(doc.createTextNode(conf.home))
          slb.appendChild(hom)
          bin = doc.createElement("bindir")
          bin.appendChild(doc.createTextNode(conf.bindir))
          slb.appendChild(bin)
          com = doc.createElement("command")
          com.appendChild(doc.createTextNode(conf.command))
          slb.appendChild(com)
          arc = doc.createElement("arch")
          arc.appendChild(doc.createTextNode(conf.arch))
          slb.appendChild(arc)
        }
        transformerFactory = TransformerFactory.newInstance()
        transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys::INDENT, "yes")
        source = DOMSource.new(doc)
        result = StreamResult.new(fos)
        transformer.transform(source, result)
      rescue Exception => e
        exception = true
        puts e.message + "\n" + e.backtrace.join("\n")
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

  # This computes the standard matlab version i.e 7.5 from the textual version i.e. 2007b
  def matlabYearToVersion(yearab)
    maj = 7
    orig_min = 2
    orig_year = 2006
    year = yearab[0..3].to_i
    isb = (yearab[4, 1] == "b") || (yearab[4, 1] == "B")
    min = orig_min + (year - orig_year) * 2 + (isb ? 1 : 0)
    return "#{maj}.#{min}"
  end


  def matlabVersion(home)
    version = `#{bin} -version`
    if $?.to_i == 256

      version.each_line do |l|

        if l["scilab-"]
          return scilabVersionAndArchClean(l)
        end
      end
    end
  end

  def matlabVersionAndArchClean(version)
    return version.gsub(/scilab-|\(64-bit\)/, '').strip(), version.index("(64-bit)") ? "64" : "32"
  end

  def matlabBinDir(is64dir)
    case MatSciFinder.os
      when :windows
        if is64dir
          "bin\\win64"
        else
          "bin\\win32"
        end
      when :linux
        if arch64?
          return "bin/glnxa64"
        else
          "bin/glnx86"
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

  def matlabCommandName
    case MatSciFinder.os
      when :windows
        "MATLAB.exe"
      when :linux
        if arch64?
          "matlab"
        else
          "matlab"
        end
      when :macosx
        if arch64?
          "matlab"
        else
          "matlab"
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


selected = false
#args=["forceSearch", "true"]

mf = MatSciFinder.new
begin

  mf.parseParams()
  conffound = false
  if !mf.forceSearch
    conffound = mf.readConfigs()
  end
  if !conffound
    conffound = mf.findMatlab()

    mf.writeConfigs()

  end
  if conffound
    selected = mf.chooseConfig()
  else
    puts "no Matlab installation found"
  end

rescue Exception => e
  puts e.message + "\n" + e.backtrace.join("\n")
  raise java.lang.RuntimeException.new(e.message + "\n" + e.backtrace.join("\n"))
ensure
  begin
    mf.close()
  rescue Exception => e
    puts e.message + "\n" + e.backtrace.join("\n")
    raise java.lang.RuntimeException.new(e.message + "\n" + e.backtrace.join("\n"))
  end
end
