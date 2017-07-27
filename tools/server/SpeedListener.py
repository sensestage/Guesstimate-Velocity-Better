#!/usr/bin/python

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


import smtplib
import email.mime.text, email.utils

class WarningEmailer(object):
	"""Send a warning email to all registered recipients
	"""
	def __init__(self, smtphost):
		self.smtp = smtplib.SMTP(smtphost)

		self.recipients = []

		self.subject = "CVBS Warning"

		self.sender = "CVBS Server <spamxxx@example.com>"

	def set_subject(self, subject):
		self.subject = subject

	def set_sender(self, sender):
		(name, addr) = email.utils.parseaddr(sender)
		if not len(addr):
			raise(ValueError("Not a valid email-address: %s" % sender))

		self.sender = sender

	def add_recipient(self, recipient):
		addr = email.utils.parseaddr(recipient)[1]
		if not len(addr):
			raise(ValueError("Not a valid email-address: %s" % recipient))

		if not addr in self.recipients:
			self.recipients.append(addr)

	def remove_recipient(self, recipient):
		addr = email.utils.parseaddr(recipient)[1]
		if not len(addr):
			raise(ValueError("Not a valid email-address: %s" % recipient))

		self.recipients.remove(addr)

	def add_recipients(self, recipients):
		for recipient in recipients:
			self.add_recipient(recipient)

	def send_message(self, body):
		msg = email.mime.text.MIMEText(body)
		msg['Subject'] = self.subject
		msg['From'] = self.sender
		msg['To'] = ", ".join(self.recipients)

		self.smtp.sendmail(self.sender, self.recipients, msg.as_string())


import datetime, struct, time, datetime, threading, logging
import SocketServer

class SpeedRequestHandler(SocketServer.StreamRequestHandler):
	"""Handles incoming Speed-data from the clients
	"""
	def handle(self):
		id_str = self.rfile.read(1)
		if not len(id_str):
			logging.error("Got an empty packet")
			return

		try:
			client_id = int(id_str)
		except ValueError:
			logging.error("Got an invalid client_id: %s" % id_str)
			return

		logging.debug("Got data-packet from client %d" % client_id)
		if client_id not in self.server.writers:
			self.server.create_writer(client_id)

		self.server.last_seen[client_id] = datetime.datetime.now()

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
		self.last_seen = {}

		self.client_timeout = datetime.timedelta(minutes=10)

		self.datadir = data_dir
		if not os.path.exists(data_dir):
			os.makedirs(data_dir)

	def client_timeout_callback(self, client_id, last_seen):
		logging.warn("Client %d not seen since %s" % (client_id, last_seen.strftime("%Y-%m-%d %H:%M:%S")))

	def client_timeout_checker(self):
		while self.is_alive():
			now = datetime.datetime.now()
			for (client_id, last_seen) in self.last_seen.items():
				if last_seen < (now - self.client_timeout):
					self.client_timeout_callback(client_id, last_seen)
					del self.last_seen[client_id]

			time.sleep(1)

	def set_client_timeout(self, minutes):
		self.client_timeout = datetime.timedelta(minutes=minutes)

	def is_alive(self):
		return (hasattr(self.server_thread, "is_alive") and self.server_thread.is_alive())

	def start(self):
		self.today_dir = os.path.join(self.datadir, datetime.date.isoformat(datetime.date.today()))
		if not os.path.exists(self.today_dir):
			os.makedirs(self.today_dir)

		if self.is_alive():
			raise(Exception("Server thread '%s' already started" % self.server_thread.name))

		self.server_thread = threading.Thread(target=self.serve_forever, name="main server thread")
		self.server_thread.start()

		self.client_checker_thread = threading.Thread(target=self.client_timeout_checker, name="client checker thread")
		self.client_checker_thread.start()

		logging.debug("Server started")

	def shutdown(self):
		if self.is_alive():
			super(SpeedListener, self).shutdown()

		self.server_thread.join()
		self.client_checker_thread.join()
		logging.debug("Server stopped")
		self.socket.close()
		for wr in self.writers.values():
			wr.close()

	def create_writer(self, client_id):
		filename = os.path.join(self.today_dir, "tram-%d.txt" % client_id)
		self.writers[client_id] = SpeedWriter(filename)
		logging.debug("created SpeedWriter for %s" % filename)


import socket, random

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
				sock.sendall(str(self.client_id) + "\n".join(self.buf))
				sock.close()

		finally:
			print("Quitting")


import ConfigParser

class ConfigFile(object):
	"""Parse the configuration-file
	"""

	defaults = {
		'listener':{
			'port':5858,
			'datadir':'/var/cvbs/data',
			'client_timeout':10,
			'logfile':'/var/cvbs/log/cvbs.log',
			'loglevel':3,
			'verbose':0 },
		'fans':{
			'device_ids':'1,2,3,4,5,6',
			'client_ids':'1,2,3,4,5,6',
			'min_speed':1.0,
			'min_pct':6.0,
			'max_speed':80.0,
			'max_pct':100.0,
			'logfile':'',
			'loglevel':3,
			'verbose':0 },
		'emailer':{
			'smtphost':'smtp.xs4all.nl',
			'sender':'CVBS Server <spamxxx@example.com>',
			'recipients':'CVBS Server <spamxxx@example.com>',
			'subject':'CVBS Warning' }
	}

	def __init__(self, config_file):

		self.parser = ConfigParser.SafeConfigParser()
		self.read(config_file)

	def read(self, filename):
		self.filename = filename
		if os.path.isfile(filename):
			self.parser.read(filename)
		else:
			self._set_defaults()
			self.write()

	def write(self, filename=None):
		if filename == None:
			fo = open(self.filename, 'w')
		else:
			fo = open(filename, 'w')
			self.filename = filename

		self.parser.write(fo)
		fo.close()

	def _set_defaults(self):
		for (section, options) in self.defaults.items():
			if not self.parser.has_section(section):
				self.parser.add_section(section)

			for (option, value) in options.items():
				self.parser.set(section, option, str(value))

	def _parse_value(self, value):
		if ',' in value:
			values = value.split(',')
			out = []
			for v in values:
				out.append(self._parse_value(v.strip()))

			return out

		try:
			return int(value)
		except ValueError:
			try:
				return float(value)
			except ValueError:
				return value

	def get(self, section, option=None, raw=0):
		if option == None:
			return self.items(section, raw)

		return self._parse_value(self.parser.get(section, option, raw))

	def items(self, section, raw=0):
		items = self.parser.items(section, raw)
		out = {}
		for (option, value) in items:
			out[option] = self._parse_value(value)

		return out


if __name__ == '__main__':
	import optparse, signal
	import logging, logging.handlers

	default_configfile = "/var/cvbs/cvbs.cfg"

	# create and populate command-line option parser
	op = optparse.OptionParser()

	op.add_option("-c", "--config-file", action="store", type="string", dest="config_file", metavar="FILE",
				  help="load alternate configuration from FILE [%s]" % default_configfile, default=default_configfile)
	op.add_option("-v", "--verbose", action="store", type="int", dest="verbose", metavar="LEVEL",
				  help="log-level (0 - 5) for messages to stderr [4]", default=4)
	op.add_option("-l", "--loglevel", action="store", type="int", dest="loglevel", metavar="LEVEL",
				  help="log-level (0 - 5) [3]")
	op.add_option("-f", "--logfile", action="store", type="string", dest="logfile", metavar="FILE",
				  help="write log-messages to FILE")
	op.add_option("-p", "--port", action="store", type="int", dest="port",
				  help="TCP port to listen on [5858]")
	op.add_option("-d", "--datadir", action="store", type="string", dest="datadir", metavar="DIR",
				  help="The root directory of the tram-speed data-file tree [/var/cvbs/data]")
	op.add_option("-t", "--timeout", action="store", type="float", dest="timeout", metavar="MIN",
				  help="report missing data-packets from clients after MIN minutes")
	op.add_option("-r", "--recipient", action="append", type="string", dest="recipients", metavar="EMAIL",
				  help="add EMAIL address to the list of recipients of warning-emails")
	op.add_option("--smtphost", action="store", type="string", dest="smtphost", metavar="HOST",
				  help="send emails using the smtp-server HOST")
	op.add_option("--from", action="store", type="string", dest="sender", metavar="EMAIL",
				  help="send emails from this EMAIL address")
	op.add_option("--subject", action="store", type="string", dest="subject",
				  help="send emails with this SUBJECT")

	# parse options
	(opts, args) = op.parse_args()

	# load config-file
	cf = ConfigFile(opts.config_file)
	listener_conf = cf.items('listener')
	emailer_conf = cf.items('emailer')

	# command-line options override (but don't overwrite) values from config-file
	if opts.port != None:
		listener_conf['port'] = opts.port

	if opts.datadir != None:
		listener_conf['datadir'] = opts.datadir

	if opts.timeout != None:
		listener_conf['client_timeout'] = opts.timeout

	if opts.logfile != None:
		listener_conf['logfile'] = opts.logfile

	if opts.loglevel != None:
		listener_conf['loglevel'] = opts.loglevel

	if opts.recipients != None:
		emailer_conf['recipients'].extend(opts.recipients)

	if opts.smtphost != None:
		emailer_conf['smtphost'] = opts.smtphost

	if opts.sender != None:
		emailer_conf['sender'] = opts.sender

	if opts.subject != None:
		emailer_conf['subject'] = opts.subject

	# set up logging
	logging.basicConfig(level=listener_conf['verbose'] * 10,
						format="%(asctime)s - %(levelname)-8s: %(message)s",
						datefmt="%Y-%m-%d %H:%M:%S")

	if len(listener_conf['logfile']):
		(logdir, basename) = os.path.split(listener_conf['logfile'])
		if not os.path.isdir(logdir):
			os.makedirs(logdir)

		logfile = logging.handlers.TimedRotatingFileHandler(os.path.join(logdir, basename), when='midnight')
		logformatter = logging.Formatter("%(asctime)s - %(levelname)-8s: %(message)s", "%Y-%m-%d %H:%M:%S")
		logfile.setLevel(listener_conf['loglevel'] * 10)
		logfile.setFormatter(logformatter)
		logging.getLogger('').addHandler(logfile)


	# create SpeedListener server
	srv = SpeedListener(('', listener_conf['port']), listener_conf['datadir'], listener_conf['client_timeout'])

	mailer = WarningEmailer(emailer_conf['smtphost'])
	mailer.set_sender(emailer_conf['sender'])
	mailer.set_subject(emailer_conf['subject'])
	mailer.add_recipients(emailer_conf['recipients'])

	def graceful_exit_handler(signum, frame):
		logging.warn("Caught signal %d" % signum)
		srv.shutdown()

	def client_timeout_callback(client_id, last_seen):
		msg = "Client %d not seen since %s" % (client_id, last_seen.strftime("%Y-%m-%d %H:%M:%S"))
		logging.warn(msg)
		try:
			mailer.send_message(msg)
		except Exception, e:
			logging.error("Sending email failed: %s" % str(e))

	srv.client_timeout_callback = client_timeout_callback

	signal.signal(signal.SIGQUIT, graceful_exit_handler)
	signal.signal(signal.SIGTERM, graceful_exit_handler)

	srv.start()

	try:
		while srv.is_alive():
			time.sleep(10)
	except KeyboardInterrupt:
		graceful_exit_handler(signal.SIGINT, 0)
