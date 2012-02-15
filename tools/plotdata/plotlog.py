#!/usr/bin/env python
# -*- coding: utf-8 -*-

import optparse
# from Python v2.7 on should become argparse

#from numpy import *
from enthought.chaco.shell import *

from enthought.traits.api import HasTraits, Instance
from enthought.traits.ui.api import View, Item
from enthought.chaco.api import GridPlotContainer, VPlotContainer, Plot, ArrayPlotData
from enthought.enable.component_editor import ComponentEditor
#from numpy import linspace, sin

import numpy
import math
import csv

class ContainerHor(HasTraits):

    plot = Instance(VPlotContainer)

    traits_view = View(Item('plot', editor=ComponentEditor(), show_label=False),
                       width=1000, height=600, resizable=True, title="Chaco Plot")

    def __init__(self, plot1,plot2,plot3):
        super(ContainerHor, self).__init__()

        container = VPlotContainer(plot1,plot2,plot3)
        self.plot = container

class ContainerHor4(HasTraits):

    plot = Instance(VPlotContainer)

    traits_view = View(Item('plot', editor=ComponentEditor(), show_label=False),
                       width=1000, height=800, resizable=True, title="Chaco Plot")

    def __init__(self, plot1,plot2,plot3, plot4):
        super(ContainerHor4, self).__init__()

        container = VPlotContainer(plot1,plot2,plot3,plot4)
        self.plot = container

class ContainerGrid(HasTraits):

    plot = Instance(GridPlotContainer)

    traits_view = View(Item('plot', editor=ComponentEditor(), show_label=False),
                       width=1000, height=1000, resizable=True, title="All plots")

    def __init__(self, plot1,plot2,plot3, plot4,plot5,plot6):
        super(ContainerGrid, self).__init__()

        container = GridPlotContainer(plot1,plot2,plot3, plot4,plot5,plot6)
        print container.shape
        self.plot = container

#class LinePlot(HasTraits):
    #plot = Instance(Plot)
    #traits_view = View(
        #Item('plot',editor=ComponentEditor(), show_label=False),
        #width=500, height=500, resizable=True, title="Chaco Plot")

    #def __init__(self, x, y, title ):
        #super(LinePlot, self).__init__()

	##x = marray2[0]
	##y = marray2[1]
	##y2 = marray2[4]
	##y3 = marray2[5]
  
        #plotdata = ArrayPlotData(x=x, y=y ) #, y2=y2, y3=y3 )
        #plot = Plot(plotdata)
        #plot.plot(("x", "y"), type="line", color="blue")
        #plot.title = title
        ##self.plot = plot

def makeplot( x,y,title ):
  plotdata = ArrayPlotData(x=x, y=y ) #, y2=y2, y3=y3 )
  plot = Plot(plotdata)
  plot.plot(("x", "y"), type="line", color="blue")
  plot.title = title
  return plot


def online_variance(data):
    n = 0
    mean = 0
    M2 = 0
 
    for x in data:
        n = n + 1
        delta = x - mean
        mean = mean + delta/n
        if n > 1:
            M2 = M2 + delta*(x - mean)
 
    variance_n = M2/n
    variance = M2/(n - 1)
    return (mean, variance, variance_n)


def rolling_window(a, window):
    shape = a.shape[:-1] + (a.shape[-1] - window + 1, window)
    strides = a.strides + (a.strides[-1],)
    return numpy.lib.stride_tricks.as_strided(a, shape=shape, strides=strides)
    
    
def motion_state_machine( meanF, stdF, meanS, stdS, deltaT ):
  # forward motion: meanF, stdF ; sideways motion: meanS, stdS ; mean and standard deviation
  global motion_state
  global still_count
  global motion_state_index
  if motion_state == 'still':
    if stdF > 0.3:
      motion_state = 'accelerating'
      motion_state_index = 2.
  elif motion_state == 'accelerating':
    if meanF < 0.5:
      motion_state = 'steady_motion'
      motion_state_index = 1.
  elif motion_state == 'steady_motion':
    if stdF > 0.25 and meanF < -0.5:
      motion_state = 'decelerating'
      motion_state_index = 3.
  #elif motion_state == 'decelerating':
    #if stdS < 0.1 and stdF < 0.1:
      #motion_state = 'still'
      #motion_state_index = 0.
  else:
    if stdS < 0.1 and stdF < 0.1:
      still_count = still_count + deltaT * 0.001
      if still_count > 3.:
	still_count = 0.
	motion_state = 'still'
	motion_state_index = 0.
      
def calculate_speed( meanF, deltaT ):
  global speed
  if motion_state == 'still':
    speed = 0.
  else: #moving
    speed += meanF * deltaT * 0.001 * 3.6
  #speed = speed * 3.6 # km/h
    
def substract_offset( value, offset ):
  return value - offset

def openFile( filename ):
	global motion_state
	global motion_state_index
	global speed
	global still_count
	still_count = 0.
	speed = 0.
	motion_state_index = 0.
	motion_state = 'still'
	csvfile = open(filename, "rb")
	#dialect = csv.Sniffer().sniff(csvfile.read(1024))
	#csvfile.seek(0)
	reader = csv.reader(csvfile, delimiter=' ')
	#reader = csv.reader(open(filename, 'rb'), dialect='excel-tab')
        rowlength = 0
	rows = []
	diffrows = []

	for row in reader:
	  if len( row ) >= rowlength:
	    rowlength = len( row )
	    rows.append( list(map(float, row) ) )
	    #diffrows.append( list(map(float, [ row[1], row[4] ]) ) )

	#print rows
	#print diffrows
	#print len(rows), len(diffrows)
	
	marray = numpy.asarray( rows )
	#diffarray = numpy.asarray( diffrows )

	#marray1 = marray[ marray[:,1] == 1]
	#marray2 = marray[ marray[:,1] == 2]

	#diffarray1 = diffarray[ marray[:,1] == 1]
	#diffarray2 = diffarray[ marray[:,1] == 2]

	#hasNode1 = False
	#hasNode2 = False

	mplot = marray.transpose(1,0)

	#plotX = makeplot( mplot[0], mplot[1], "X" )
	#plotY = makeplot( mplot[0], mplot[2], "Y" )
	#plotZ = makeplot( mplot[0], mplot[3], "Z" )
	#ContainerHor(plotX,plotY,plotZ).configure_traits()
	
	overallmean = numpy.mean( mplot[1:4], -1 )
	#print overallmean
	#print overallmean.shape
	
	#print mplot.shape
	
	deltatimes = numpy.diff( mplot[0] )
	deltaStride = rolling_window( deltatimes, 200 )
	meanDT = numpy.mean( deltaStride, -1 )
	stdDT = numpy.std( deltaStride, -1 )
	
	mplotCorr = mplot[1:4] - overallmean[:, numpy.newaxis]
	#mplotCorr = map( x - overallmean , mplot[1:4] )
	#print mplotCorr.shape
	
	#mplotStride = rolling_window( mplot[1:4], 200 )
	mplotStride = rolling_window( mplotCorr, 200 )
	#print mplotStride.shape
	#print mplotStride.transpose(1,0).shape
	
	means = numpy.mean( mplotStride, -1 )
	#print means.shape
	
	stds = numpy.std( mplotStride, -1 )
	#print stds.shape
	
	print meanDT.size
	## calculate motion
	speeds = []
	motions = []
	for i in range( 1, meanDT.size ):
	  #print i
	  motion_state_machine( means[1][i], stds[1][i], means[0][i], stds[0][i], meanDT[i] )
	  calculate_speed( means[1][i], meanDT[i] )
	  #calculate_speed( means[1][i], deltatimes[i] )
	  speeds.append( speed )
	  motions.append( motion_state_index )
	  #print speed
	mspeeds = numpy.asarray( speeds )
	mmotion = numpy.asarray( motions )

	#plotDT = makeplot( mplot[0], deltatimes, "delta Times" )
	#plotDTm = makeplot( mplot[0], meanDT, "DT mean" )
	#plotDTs = makeplot( mplot[0], stdDT, "DT std" )
	#plotZm = makeplot( mplot[0], means[2], "Z mean" )
	#ContainerHor(plotDT,plotDTm,plotDTs).configure_traits()

	timeaxis = mplot[0] * 0.001 / 60


	plotXm = makeplot( timeaxis, means[0], "X mean" )
	plotYm = makeplot( timeaxis, means[1], "Y mean" )
	plotZm = makeplot( timeaxis, means[2], "Z mean" )
	#ContainerHor(plotXm,plotYm,plotZm).configure_traits()

	plotXs = makeplot( timeaxis, stds[0], "X std" )
	plotYs = makeplot( timeaxis, stds[1], "Y std" )
	plotZs = makeplot( timeaxis, stds[2], "Z std" )
	#ContainerHor(plotXs,plotYs,plotZs).configure_traits()
	
	plotS = makeplot( timeaxis, mspeeds, "speed" )
	plotMo = makeplot( timeaxis, mmotion, "motion" )
	
	ContainerHor4(plotYm,plotYs,plotS,plotMo).configure_traits()
	ContainerHor4(plotYm,plotYs,plotXm,plotXs).configure_traits()
	
	#ContainerGrid(plotXm,plotYm,plotZm,plotXs,plotYs,plotZs).configure_traits()



if __name__ == "__main__":

  parser = optparse.OptionParser(description='Plot the log.')
  parser.add_option('-f','--file', action='store', type="string", dest="filename",default="",
		  help='the name of the log file to plot [default:%s]'% '')

  (options,args) = parser.parse_args()
  #print args.accumulate(args.integers)
  #print options
  #print args
  #print( options.host )
  openFile( options.filename )

  #mylineplot = LinePlot( options.filename ).configure_traits()
