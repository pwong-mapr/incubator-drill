#!/bin/sh

category=""
defSources=""
groups=""

#
# functions
#
usage()
{
    echo >&2 "$1"

    cat  << EOF

$0 -c <category> [-s <defSources>] [-g <groups>]

    -c <category> : required
    -s <defSources> : optional
    -g <groups> : optional

    Valid values for category:
      positiveTests
      negativeTests

    testDefSource - test definition file/directory.  If not provided, all tests under resources will run.  Examples:
      basic_query/testcases/select/select.json
      /root/basic_query/testcases/select/select.json

    testGroups - optional.  When not specified, all test groups will be executed.  Examples:
      smoke
      smoke,regression

EOF
    exit 1
}


##
## MAIN
##
if [ $# = 0 ]; then
    usage "No arguements given"
fi

while [ $# -gt 0 ]; do

    case $1 in
        #
        # category is required
        #
        -c) 
            category=$2
            shift
            ;;

        #
        # defSources is optional 
        #
        -s)
            defSources=$2
            shift
            ;;

        #
        # groups is optional
        #
        -g) 
            groups=$2
            shift
            ;;

        #
        # invalid option
        #
        *)
            usage "Invalid argument: $1"
            ;;
    esac
    shift
done

#
# check to see if category was set
#
if [ -z "$category" ]; then
    usage "-c <category> is required"
fi
      
#
# run the command with the given arguments
# 
command="mvn clean test -Dtest=org.apache.drill.test.framework.DrillTestsMapRCluster#$category"

if [ -n "$defSources" ]; then
    echo "getting defSource......."
    command=$command" -Dtest.def.sources=$defSources"
fi

if [ -n "$groups" ]; then
    command=$command" -Dtest.groups=$groups"
fi

echo "Running $command"
$command
