#!/bin/sh

category=""
defSources=""
jdbcUrl=""
groups=""

#
# functions
#
usage()
{
    echo >&2 "$1"

    cat  << EOF

$0 -c <category> -s <defSources> [-u <jdbcUrl>] [-g <groups>]

    -c <category> : required
    -s <defSources> : required
    -u <jdbcUrl> : optional
    -g <groups> : optional

    Valid values for testCategory:
      positiveTests
      negativeTests

    testDefSource - test definition file/directory.  Examples:
      basic_query/testcases/select/select.json
      /root/basic_query/testcases/select/select.json

    jdbcUrl - JDBC connection URL; required only when queries are submitted via JDBC.  Examples:
      jdbc.drill:

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
        # defSources is required
        #
        -s)
            defSources=$2
            shift
            ;;

        #
        # jdbcUrl is optional
        #
        -u)
            jdbcUrl=$2
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
# check to see if defSources was set
#
if [ -z "$defSources" ]; then
    usage "-s <defSources> is required"
fi

#
# run the command with the given arguments
# 
command="mvn clean test -Dtest=org.apache.drill.test.drilltests.DrillPerformanceTests#$category -Dtest.def.sources=$defSources"

if [ -n "$jdbcUrl" ]; then
    command=$command" -Djdbc.connection.url=$jdbcUrl"
fi

if [ -n "$groups" ]; then
    command=$command" -Dtest.groups=$groups"
fi

echo "Running $command"
$command
