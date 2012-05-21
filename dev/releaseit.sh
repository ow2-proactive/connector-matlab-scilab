#!/bin/sh

# Call this script to build the release once RCPs have been built first.
# This script calls dev/release/release-rm, dev/release/release-all, and makeRCP_arch.sh scripts to completly build the whole release.
# At the end, full Scheduling release can be found in 'destination' directory argument.

#usage if args number < 3
if [ $# -lt 3 ]
then
    echo usage : $0 root_directory version destination [java_home]
	echo
	echo "    root_directory   : Root directory of the MatSci project to be released : directory that contains license files"
	echo "    version          : Version number to release"
	echo "    destination      : Destination path for the final generated archive"
	echo "    java_home        : java_home path (optional - default will be '$JAVA_HOME')"
	echo
	echo "    Example :"
	echo "    $0 /home/Workspace/Matlab_Scilab_Connector 1.0.0 /home/Public/Matlab_Scilab_Connector-1.0.0"
	echo "    "
	echo
	echo
	echo "    Note : To have a special archive name that is not only the version number to release, just set the sysenv var PAS_RELEASE_NAME"
	echo "           with the name which will replace version number is the archive name"
	exit
fi


#assign arguments
ROOT_DIRECTORY=$1
VERSION=$2
OUTPUT_DIRECTORY=$3
if [ "$#" -eq "4" ]
then
	JAVA_HOME_u=$4
else
	JAVA_HOME_u=$JAVA_HOME
fi


# CHECK ROOT DIRECTORY ARGUMENT
if [ ! -d "$ROOT_DIRECTORY" ] && [ ! -e "$ROOT_DIRECTORY/LICENSE.txt" ]
then
	echo "'$ROOT_DIRECTORY' is not a valid Matlab/Scilab Connector root directory"
	exit
fi


#ask user if ready 
echo "*"
echo "*  Release is now ready to be built"
echo "*"
echo "*   Note : To have a special archive name that is not only the version number to release, just export the sysenv var 'PAS_RELEASE_NAME'"
echo "*          with the name which will replace version number is the archive name"
echo "*"
echo "*  Read $ROOT_DIRECTORY/dev/release/HOWTO_Matlab_Scilab_Connector.txt for more details about the release process."
echo "*  Read it now ? (y/n)."
read answer
#check answer, if no 'y' -> exit
if [ "$answer" == "y" ]
then 
	echo "--------------------"
	more $ROOT_DIRECTORY/dev/release/HOWTO_Matlab_Scilab_Connector.txt
	echo "--------------------"
fi
echo " "
echo "Are you sure you want to build the release now ? (y/n)"
read answer
#check answer, if no 'y' -> exit
if [ "$answer" != "y" ]
then 
	echo "Aborting... Nothing was done"
	exit
fi


#RELEASE IT -------------------------------
#go into matsci root dir
cd $ROOT_DIRECTORY
#release RESOURCING first, then SCHEDULING
echo "---------------> 1. Release-rm"
dev/release/release-rm . $VERSION $JAVA_HOME_u
echo "---------------> 2. Release-all"
dev/release/release-all . $VERSION $JAVA_HOME_u
#move create archive in destination dir
echo "---------------> 3. Move rm and scheduler server archive to '$OUTPUT_DIRECTORY'"
SPECIAL_NAME=$VERSION
if [ "$PAS_RELEASE_NAME" != "" ]
then
	SPECIAL_NAME=$PAS_RELEASE_NAME
fi
mv /tmp/ProActiveResourcing-${SPECIAL_NAME}_*.tar.gz $OUTPUT_DIRECTORY
mv /tmp/ProActiveResourcing-${SPECIAL_NAME}_*.zip $OUTPUT_DIRECTORY
mv /tmp/ProActiveScheduling-${SPECIAL_NAME}_*.tar.gz $OUTPUT_DIRECTORY
mv /tmp/ProActiveScheduling-${SPECIAL_NAME}_*.zip $OUTPUT_DIRECTORY
#change dir to dev/release
echo "---------------> 4. Change directory to dev/release"
cd dev/release;
#update RCPs content (add scripts, update launcher init, etc...)
echo "---------------> 5. Update RCPs content"
makeRCP_arch.sh /tmp/ProActiveScheduling-${SPECIAL_NAME}_server $RCPs_DIRECTORY ${VERSION} $OUTPUT_DIRECTORY
#remove remaining temporary server directories
echo "---------------> 6. Remove remaining temporary directories ? y/n"
echo "                       /tmp/ProActiveResourcing-${SPECIAL_NAME}_*"
echo "                       /tmp/ProActiveScheduling-${SPECIAL_NAME}_*"
read answer
#check answer, if 'y' -> remove
if [ "$answer" == "y" ]
then 
    rm /tmp/ProActiveResourcing-${SPECIAL_NAME}_* -rf /tmp/ProActiveScheduling-${SPECIAL_NAME}_*
fi
echo "---------------> 7. End of release process"

