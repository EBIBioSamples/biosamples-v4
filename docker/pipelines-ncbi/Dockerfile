FROM biosamples:latest
MAINTAINER EBI BioSamples <biosamples@ebi.ac.uk>

COPY run.sh /run.sh

RUN apk add --no-cache wget ca-certificates

ENTRYPOINT [ "sh","-a","-v","-c"]
CMD [ "/run.sh" ]