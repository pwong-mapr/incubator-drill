#/bin/bash
label=$1
uniqID=${label}_$(date +%Y%m%d_%H%M%S)
echo "Started Tracking At $uniqID"

nohup ./trackResources.sh $uniqID &> nohup.out &
