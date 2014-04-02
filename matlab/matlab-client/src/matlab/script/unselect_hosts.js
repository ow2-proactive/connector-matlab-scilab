selected = true;
for (i=0; (i < args.length) && selected; i++) {
    exclusionString=args[i];
    print("Verif : hostname "+ java.net.InetAddress.getLocalHost().getHostName()+" must not contain  : "+ exclusionString+"\n");
    selected = selected && !java.net.InetAddress.getLocalHost().getHostName().matches(".*"+exclusionString+".*");
}

