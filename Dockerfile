FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/porra-worldcup.jar /porra-worldcup/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/porra-worldcup/app.jar"]
