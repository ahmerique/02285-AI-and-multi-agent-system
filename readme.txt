Compiling the searchclient:
    javac src/searchclient/SearchClient.java -cp

Starting the server using the searchclient:
    java -jar server.jar -l levels/SAD1.lvl -c "java -Xmx2g searchclient.SearchClient" -g 150 -t 300