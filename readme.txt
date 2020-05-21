Compiling the searchclient:
    javac src/searchclient/SearchClient.java

Starting the server using the searchclient:
    java -jar server.jar -l levels/SAD1.lvl -c "java -Xmx2g src.searchclient.SearchClient" -g 150 -t 300

Starting all competition levels:
    java -jar server.jar -c "java -Xmx2g src.searchclient.SearchClient" -l "complevels" -t 180 -o "Baguettes.zip"