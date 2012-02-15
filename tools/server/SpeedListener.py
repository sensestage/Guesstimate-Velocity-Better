#!/usr/bin/python
# -*- coding: utf-8 -*-

class RingBuffer(list):
	"""An extension of the built-in list object that is given a maximum length
	if the buffer is appended to or otherwise extended beyond the max length, items are
	deleted from the front of the buffer
	"""
	def __init__(self, sequence=[], max_length=2):
		super(RingBuffer, self).__init__(sequence)

		self._max_length = max_length
		self._shrink_()

	@property
	def max_length(self):
		"""The maximum length of the buffer"""
		return self._max_length

	@max_length.setter
	def max_length(self, length):
		if length < 1:
			raise(ValueError("Buffer max_length must be 1 or greater"))

		self._max_length = length
		self._shrink_()

	def _shrink_(self):
		while len(self) > self._max_length:
			del self[0]

	def append(self, item):
		super(RingBuffer, self).append(item)
		self._shrink_()

	def extend(self, items):
		super(RingBuffer, self).extend(items)
		self._shrink_()

	def insert(self, index, item):
		super(RingBuffer, self).insert(index, item)
		self._shrink_()

	def __iadd__(self, other):
		super(RingBuffer, self).__iadd__(other)
		self._shrink_()
		return self

import os

class SpeedWriter(object):
	"""writes received tram-speeds into a file"""
	def __init__(self, filename):
		(dirname, basename) = os.path.split(filename)
		if not os.path.exists(dirname):
			os.makedirs(dirname)

		self.fobj = open(os.path.join(dirname, basename), 'a')

		self.buf = RingBuffer(max_length=120)

	def write(self, entries):
		for entry in entries:
			if entry in self.buf:
				continue

			self.buf.append(entry)
			self.fobj.write(entry + "\n")

		self.fobj.flush()

	def close(self):
		self.fobj.close()


import datetime, struct, threading, logging
import SocketServer

class SpeedRequestHandler(SocketServer.StreamRequestHandler):
	"""Handles incoming Speed-data from the clients
	"""
	def handle(self):
		#client_id = struct.unpack("<B", self.rfile.read(1))[0]
		client_id = self.rfile.read(1)
		if len( client_id ) > 0:
		  client_id = int( client_id )
		else:
		  client_id = 0
		logging.debug("Got data-packet from client %d" % client_id )
		if client_id not in self.server.writers:
			self.server.create_writer(client_id)

		packet = self.rfile.read()
		entries = packet.split("\n")

		self.server.writers[client_id].write(entries)

class SpeedListener(SocketServer.ThreadingMixIn, SocketServer.TCPServer, object):
	"""A multi-threaded TCP-server that listens for incoming Speed-data from clients
	"""
	def __init__(self, address, data_dir="/var/cvbs/data", timeout=10):
		super(SpeedListener, self).__init__(address, SpeedRequestHandler)

		self.timeout = timeout
		self.server_thread = None

		self.writers = {}

		self.datadir = data_dir
		if not os.path.exists(data_dir):
			os.makedirs(data_dir)

	def start(self):
		self.today_dir = os.path.join(self.datadir, datetime.date.isoformat(datetime.date.today()))
		if not os.path.exists(self.today_dir):
			os.makedirs(self.today_dir)

		if hasattr(self.server_thread, "is_alive") and self.server_thread.is_alive():
			raise(Exception("Server thread '%s' already started" % self.server_thread.name))

		self.server_thread = threading.Thread(target=self.serve_forever, name="main server thread")
		self.server_thread.start()
		logging.debug("Server started")

	def shutdown(self):
		super(SpeedListener, self).shutdown()
		self.server_thread.join()
		logging.debug("Server stopped")
#		self.socket.close()
		for wr in self.writers.values():
			wr.close()

	def create_writer(self, client_id):
		filename = os.path.join(self.today_dir, "tram-%d.txt" % client_id)
		self.writers[client_id] = SpeedWriter(filename)
		logging.debug("created SpeedWriter for %s" % filename)


import socket, random, time

class SpeedSenderSimulator(object):
	"""A class to simulate sending Speed-data to the SpeedListener server
	"""
	def __init__(self, address, client_id=1):
		self.client_id = client_id
		self.srv_address = address
		self.buf = RingBuffer(max_length=60)

	def run(self):
		try:
			while True:
				for i in range(30):
					self.buf.append("%s %.1f" % (time.strftime("%Y-%m-%d %H:%M:%S"), random.random() * 80.))
					time.sleep(1)

				sock = socket.create_connection(self.srv_address)
				#sock.sendall(struct.pack("<B", self.client_id) + "\n".join(self.buf))
				sock.sendall( self.client_id + "\n".join(self.buf))
				sock.close()

		finally:
			self.sock.close()


if __name__ == "__main__":
	import logging
	logging.basicConfig(level=logging.DEBUG)

	import SpeedListener
	srv = SpeedListener.SpeedListener(('', 5562))
# the empty hostname-string in the server's address means 'any IP-addres'. The port the server listens on is 5345, but you can choose any port.
	srv.start()