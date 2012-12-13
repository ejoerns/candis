#!/usr/bin/env python

import argparse
import os
import re
import subprocess
import tempfile
import zipfile


def mkdex(dexfile, infiles):
	android_home = os.environ.get("ANDROID_HOME")
	if not android_home:
		raise EnvironmentError("Environment variable ANDROID_HOME not set")
	if not os.path.exists(android_home):
		raise EnvironmentError("Path \"%s\" not found" % android_home)
	cmd = "%s/platform-tools/dx --dex --verbose --output=\"%s\" %s" % (android_home, dexfile, " ".join(map(lambda f: "\"%s\"" % f, infiles)))
	print ">>", cmd
	p = subprocess.Popen(cmd, shell=True,stdout=subprocess.PIPE)
	print p.communicate()[0]


class JavaProperties:
	def __init__(self, filename):
		self._store = {}
		data = file(filename, "r").read()
		follow = False
		key = ""
		values = []
		for line in data.splitlines():
			line = line.strip()
			if not line or line.startswith("#"):
				continue


			if follow:
				values.append(line.rstrip("\\"))
			else:
				if key:
					self.addEntry(key, values)
				values = []
				key, v = line.split("=", 1)
				values.append(v.rstrip("\\"))
			follow = line.endswith("\\")
		if key:
			self.addEntry(key, values)

	def addEntry(self, key, values):
		self._store[key] = "".join(values)

	def parseValue(self, value):
		def repl(match):
			values = self.getEntry(match.groups()[0])
			if values:
				return values
			return ""
		value = re.sub("\$\{([^\}]*)\}", repl, value)
		return value
			

	def getEntry(self, key):
		if key in self._store:
			return self.parseValue(self._store[key])
		return None


class JavaProject:
	def __init__(self, path):
		self.libs = []
		self.main = None
		self.name = None
		if self.__isNetbeans(path):
			self.__loadNetbeans(path)
		elif self.__isNbAndroid(path):
			self.__loadNbAndroid(path)
	
	def __loadNetbeans(self, path):
		prop = JavaProperties(os.path.join(path, "nbproject", "project.properties"))
		self.name = prop.getEntry("application.title")
		possible = prop.getEntry("javac.classpath").split(":")
		exclude = [prop.getEntry("build.classes.dir"),]
		libfiles = []
		for lib in possible:
			libfile = os.path.join(path, lib)
			if os.path.exists(libfile):
				if os.path.isfile(libfile):
					libfiles.append(libfile)
				else:
					# what about dirs?
					pass
			else:
				print "Warning: %s does not exist" % libfile
		for libfile in libfiles:
			if "distributed" not in libfile:
				self.libs.append(libfile)

		self.main = os.path.join(path, prop.getEntry("dist.jar"))
		pass

	def __loadNbAndroid(self, path):
		prop = JavaProperties(os.path.join(path, "project.properties"))
		self.name = os.path.basename(os.path.dirname(path))
		self.main = os.path.join(path, "bin/classes.jar")
		lib = True
		i = 0
		while lib:
			i += 1
			lib = prop.getEntry("android.library.reference.%d" % i)
			if lib:
				if "distributed" in lib:
					continue
				lib = os.path.join(path,lib)
				if lib.endswith(".jar"):
					self.libs.append(lib)
				else:
					self.libs.append(JavaProject(lib).main)
				# self.__isNbAndroid(lib)
		

	def __isNetbeans(self, path):
		return os.path.exists(os.path.join(path, "nbproject", "project.properties"))

	def __isNbAndroid(self, path):
		return os.path.exists(os.path.join(path, "nbandroid"))


def mkcdb(cdbfile, serverproject, clientproject):

	server_proj = JavaProject(serverproject)
	client_proj = JavaProject(clientproject)
	properties = {}
	prop = "name=%s\n" % os.path.splitext(os.path.basename(cdbfile))[0]

	# generate dex file
	handle, path = tempfile.mkstemp(suffix=".dex")
	libs = [client_proj.main,]
	libs.extend(client_proj.libs)
	mkdex(path, libs)

	zf = zipfile.ZipFile(cdbfile, mode="w")

	# add dex to zip
	print "Compressing %s.dex: %s" % (client_proj.name, os.path.relpath(path))
	zf.write(path, "%s.dex" % client_proj.name)
	properties["droid.binary"] = "%s.dex" % client_proj.name

	# add main jar
	print "Compressing %s.jar: %s" % (server_proj.name, os.path.relpath(server_proj.main))
	zf.write(server_proj.main, "%s.jar" % server_proj.name)
	properties["server.binary"] = "%s.jar" % server_proj.name

	i = 0
	for lib in server_proj.libs:
		print "Compressing lib %d.jar: %s" % (i,os.path.relpath(lib))
		zf.write(lib, "lib%d.jar" % i)
		properties["server.lib.%d" % i] = "lib%d.jar" % i
		i += 1
	#create
	keys = properties.keys()
	keys.sort()
	for key in keys:
		prop += "%s=%s\n" % (key, properties[key])
	
	zf.writestr("config.properties", prop)
	os.unlink(path)
	zf.close()


if __name__ == "__main__":
	parser = argparse.ArgumentParser(description="Create cdb[Candis Distributed Bundle]-File with Java-Project files")
	parser.add_argument("CDB", help="Path of the new cdb-file")
	parser.add_argument("CONTROL", help="Path to the control project")
	parser.add_argument("ANDROID", help="Path to the android project")
	args = parser.parse_args()
	mkcdb(args.CDB, args.CONTROL, args.ANDROID)
