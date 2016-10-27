include Java

#require '/store/softs/LicenseSaver_1.0.0/ProActive_LicenseSaver-1.0.0-api.jar'
#require '/store/softs/LicenseSaver_1.0.0/lib/api/ProActive.jar'

java_import java.lang.System

module JavaIO
  include_package "java.io"
end
java_import java.net.InetAddress
java_import java.util.Date
java_import java.util.HashSet

class ReserveMatlab

  def initialize

    begin
      @nodeName = org.objectweb.proactive.api.PAActiveObject.getNode().getNodeInformation().getName()
    rescue Exception => e
      puts e.message + "\n" + e.backtrace.join("\n")
    end
    if @nodeName == nil
      @nodeName = "DummyNode"
    end

    @tmpPath = System.getProperty("java.io.tmpdir");

    logFileJava = JavaIO::File.new(@tmpPath, "ReserveMatlab"+@nodeName+".log");
    if !logFileJava.exists()
      logFileJava.createNewFile()
      logFileJava.setReadable(true, false)
      logFileJava.setWritable(true, false)
    end
    @orig_stdout = $stdout
    @orig_stderr = $stderr
    $stdout.reopen(logFileJava.toString(), "a")
    $stdout.sync=true
    $stderr.reopen $stdout


    @host = InetAddress.getLocalHost().getHostName();

    @sep = System.getProperty("file.separator")

  end

  def log(str)
    puts(str)
  end

  def close
    $stdout = @orig_stdout
    $stderr = @orig_stderr
  end


  def checkFeature(client, rid, login, feature)
    if client.areLicensed(feature)
      # TODO login with runAsMe
      begin
        request = LicenseRequest.new(rid, login, feature)
        tf = client.hasLicense(request);
      rescue
        return false
      end
      log(tf)
      return tf;
    else
      log("unlicensed")
    end
    return false

  end


  def checkMatlab

    log(Date.new().to_string()+" : Executing toolbox checking script on " + @host)
    begin
      java_import com.activeeon.proactive.license_saver.client.LicenseSaverClient
      java_import com.activeeon.proactive.license_saver.LicenseRequest
    rescue Exception => e
      log("Warning : Licensing proxy classes not found, license checking disabled")
      return true
    end
    if (defined? $args) && ($args.size >= 3)
      rid = $args[0]
      login = $args[1]
      serverurl = $args[2]
      $args.each do |a|
        log(a)
      end
      if serverurl == nil
        log("Warning : Licensing proxy not specified, license checking disabled")
        return true
      end
      begin
        client = LicenseSaverClient.new(serverurl)
      rescue Exception => e
        log(e.message + "\n" + e.backtrace.join("\n"))
        log("Error : Licensing proxy cannot be found at url : "+serverurl +" , host not selected")
        return false
      end
      if $args.length > 3
        feat_set = HashSet.new();

        $args[3..$args.length].each do |a|
          # The parameter contains the name of the toolbox
          tcode = a
          log(tcode)
          feat_set.add(tcode)

        end

        tf = checkFeature(client, rid, login, feat_set)

        # use the code and login to contact the proxy server for each Matlab feature
        return tf;
      else
        tcode = toolbox_code("matlab")
        feat_set = HashSet.new();
        feat_set.add(tcode)
        return checkFeature(client, rid, login, feat_set)
      end
    end
    return false;
  end
end
#t = Time.now
#$args=[""+t.usec.to_s,"fviale", "rmi://node0:1099/LicenseSaver"]
$selected = false

begin
  cm = ReserveMatlab.new

  #$selected = true
  $selected = cm.checkMatlab
  cm.log("Accepted = "+$selected.to_s)

rescue Exception => e
  puts e.message + "\n" + e.backtrace.join("\n")
  raise java.lang.RuntimeException.new(e.message + "\n" + e.backtrace.join("\n"))
ensure
  if cm != nil
    cm.close
  end
end
