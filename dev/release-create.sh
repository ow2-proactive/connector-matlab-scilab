#!/bin/sh

#argument 1 is the current version being released
VERSION=$1
#argument 2 is the type (Matlab or Scilab)
TYPE=$2
#argument 3 is the temp directory where MatSci_full directory must be (/tmp)
TMP_DIR=/tmp
if [ ! -z "$3" ] ; then
	TMP_DIR=`readlink -f $3`
fi

#############################################################################

# name of the directory that contains the full scheduling content  (also set in release-prepare.sh)
MATSCI_FULL_NAME=${TYPE}_Connector-${VERSION}_full

# releases names will be : ${PREFIX}${VERSION}$[SUFFIX}.ext

PREFIX_API=${TYPE}_Connector-
PREFIX_SRC=${TYPE}_Connector-

SUFFIX_SRC=_src
SUFFIX_BINARY=_bin


#############################################################################
#############################################################################

function del_dist(){
	echo "Removing all file related to dist"
	rm -rf dist
	rm -rf doc/built
}

function del_src(){
	echo "Removing all file related to sources"
	rm -rf compile classes dev doc junitReports lib src matlab scilab
    echo "moving ${TYPE} dist dir"
    lower_type=$(echo ${TYPE,,})
    mv dist/${lower_type}/* .
    rm -rf dist
}



#############################################################################

function warn_print_usage_and_exit {
	echo "$1" 1>&2
	echo "" 1>&2
	echo "Usage: $0 VERSION TMP_DIR" 1>&2
	echo "       VERSION : current version to be released" 1>&2
	echo "       TYPE : Matlab or Scilab" 1>&2
	echo "       TMP_DIR : directory containing matsci-full" 1>&2
	exit 1
}

function cp_r_full(){
	echo "Change dir to : $TMP_DIR"
	cd $TMP_DIR
	echo "Current location : $TMP_DIR"
	echo "Creating $1 from ${MATSCI_FULL_NAME}"
	cp -r ${MATSCI_FULL_NAME} $1 || exit 4
	echo "Change dir to : $1"
	cd $1
	echo "Current location : $1"
}

function create_archive(){
	echo "Change dir to : $TMP_DIR"
	cd $TMP_DIR
	echo "Current location : $TMP_DIR"
	echo "Create archives $1.tar.gz, $1.zip"
	tar cfz $1.tar.gz $1
	zip -qr $1.zip    $1
	echo "remove archive base directory : $1"
	rm -rf $1
}



#                       RUN BABY ! RUN !


if [ -z "$TMP_DIR" ] ; then
	warn_print_usage_and_exit "'TMP_DIR' is not defined"
fi
if [ -z "$VERSION" ] ; then
	warn_print_usage_and_exit "'VERSION' is not defined"
fi


echo "---------------> Building API archives..."

# Matlab_Scilab_Connector src ########################
echo "---------------> Creating Matlab_Scilab_Connector SRC..."
ARCHIVE_NAME=${PREFIX_SRC}${VERSION}${SUFFIX_SRC}
cp_r_full ${ARCHIVE_NAME}
del_dist
create_archive ${ARCHIVE_NAME}



# Matlab_Scilab_Connector binary ###############################
echo "---------------> Creating Matlab_Scilab_Connector bin..."
ARCHIVE_NAME=${PREFIX_API}${VERSION}${SUFFIX_BINARY}
cp_r_full ${ARCHIVE_NAME}
del_src
create_archive ${ARCHIVE_NAME}

