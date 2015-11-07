#!/bin/bash

BASEDIR=$(readlink -f $(dirname $0))
IMAGE_NAME=openfrontier/gerrit
CONTAINER_NAME=gerrit
LOCAL_SITE=$BASEDIR/gerrit_site
ATTEMPT_RESTART=false

while getopts "c:i:rs:" opt; do
    case "${opt}" in
    c)  CONTAINER_NAME=$OPTARG
        ;;
    i)  IMAGE_NAME=$OPTARG
        ;;
    r)  ATTEMPT_RESTART=true
        ;;
    s)  LOCAL_SITE=$OPTARG
        ;;
    \?) echo "Invalid option: -${OPTARG}" >&2
        exit 1
        ;;
    :)  echo "-${OPTARG} requires an argument." >&2
        exit 1
        ;;
    esac
done
shift $((OPTIND-1))

# Check for a running container
docker ps | grep $CONTAINER_NAME > /dev/null
IS_RUNNING=$?

if [ $IS_RUNNING -eq 0 ]; then
    echo "Container is already running"

    if [[ $ATTEMPT_RESTART = true ]]; then
        echo "Attempting to restart ${CONTAINER_NAME}..."
        docker restart $CONTAINER_NAME > /dev/null
        RESTARTED=$?

        if [ $RESTARTED -eq 0 ]; then
            echo "Restarted successfully"
        else
            echo "Restart FAILED!"
            exit 1
        fi
    fi
    exit 0;
fi

# Check if we can start the container
echo "Attempting to start ${CONTAINER_NAME}..."
docker start $CONTAINER_NAME > /dev/null
IS_STARTED=$?

if [ $IS_STARTED -eq 0 ]; then
    echo "Container started"
    exit 0;
elif [ $IS_STARTED -eq 1 ]; then
    echo "Container hasn't been deployed"
fi

# Create Gerrit config site if not exists
if [ ! -d $LOCAL_SITE ]; then 
    echo "Creating ${LOCAL_SITE}..."
    mkdir -p $LOCAL_SITE
fi

# Check if the image is downloaded already
echo "Checking if ${IMAGE_NAME} is already downloaded..."
docker images | grep $IMAGE_NAME
IS_IMAGE_DOWNLOADED=$?

if [ !$IS_IMAGE_DOWNLOADED ]; then
    docker pull $IMAGE_NAME
else # This shouldn't ever happen
    echo "Image is already downloaded"
fi

if [ !$IS_DEPLOYED ]; then
    docker run \
    --name $CONTAINER_NAME \
    -v $LOCAL_SITE:/var/gerrit/review_site \
    -p 8080:8080 \
    -p 29418:29418 \
    -d \
    $IMAGE_NAME
    echo "Container deployed with name ${CONTAINER_NAME}"
else
    echo "Container is already running!"
fi
