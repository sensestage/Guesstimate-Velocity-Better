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

class LinePlot(HasTraits):
    plot = Instance(Plot)
    traits_view = View(
        Item('plot',editor=ComponentEditor(), show_label=False),
        width=500, height=500, resizable=True, title="Chaco Plot")

    def __init__(self, plot ):
        super(LinePlot, self).__init__()
        
        self.plot = plot

def makeplot( x,y,title, miny, maxy ):
  plotdata = ArrayPlotData(x=x, y=y ) #, y2=y2, y3=y3 )
  plot = Plot(plotdata)
  plot.plot(("x", "y"), type="line", color="blue")
  plot.title = title
  
  spec_range = plot.value_mapper.range  
  spec_range.low = miny
  spec_range.high = maxy

  return plot

def makeplot2( x,y,title):
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
  
  if stdF > 0.1 and stdS > 0.04:
    motion_state = 'motion'
    motion_state_index = 1.
  if stdF < 0.04 and stdS < 0.04:
    motion_state = 'still'
    motion_state_index = 0.
  if stdF > 0.2 and meanF > 0.1:
    motion_state = 'accelerating'
    motion_state_index = 2.
  if stdF > 0.3 and meanF < -0.1:
    motion_state = 'decelerating'
    motion_state_index = 3.
    
      #still_count = still_count + deltaT * 0.001
      #if still_count > 3.:
	#still_count = 0.
	#motion_state = 'still'
	#motion_state_index = 0.
      
def calculate_speed( meanF, rawF, deltaT ):
  global speed
  global offset
  global motion_state
  global deltaspeed
  #global motion_still_count
  #if motion_state == 'still':
    #speed = speed * 0.99
    #offset = 0.99*offset + 0.01*meanF
  #else: #moving
    #speed += ( (rawF+meanF)/2. - offset ) * deltaT * 0.001 * 3.6    
    #limiter = 1.
    #deltaspeed = (0.05*(rawF- offset) + 0.85*(meanF- offset) ) * deltaT * 0.001 * 3.6
  deltaspeed = meanF * deltaT * 0.001 * 3.6
  if deltaspeed > 0:
    if motion_state == 'still':
      speed = speed * 0.95
    limiter = numpy.exp( -max(speed,0.)*math.pi/30. )
    speed += deltaspeed * limiter
  else:
    if motion_state == 'still':
      speed = speed * 0.95
    speed += deltaspeed*4
    #print motion_state, speed, deltaspeed
  speed = max( speed, 0. )
    
    #speed = max( speed, 0. )
  #speed = speed * 3.6 # km/h
    
#def substract_offset( value, offset ):
  #return value - offset

def openFile( filename ):
	global motion_state
	global motion_state_index
	global speed
	global offset
	global still_count
	still_count = 0.
	speed = 0.
	offset = 0.
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
	
	marray = numpy.asarray( rows )
	#diffarray = numpy.asarray( diffrows )

	mplot = marray.transpose(1,0)

	#plotX = makeplot( mplot[0], mplot[1], "X" )
	#plotY = makeplot( mplot[0], mplot[2], "Y" )
	#plotZ = makeplot( mplot[0], mplot[3], "Z" )
	#ContainerHor(plotX,plotY,plotZ).configure_traits()
	
	overallmean = numpy.mean( mplot[1:4], -1 )
	##print overallmean
	##print overallmean.shape
	
	##print mplot.shape
	
	#deltatimes = numpy.diff( mplot[0] )
	#deltaStride = rolling_window( deltatimes, 200 )
	#meanDT = numpy.mean( deltaStride, -1 )
	#stdDT = numpy.std( deltaStride, -1 )
	
	mplotCorr = mplot[1:4] - overallmean[:, numpy.newaxis]
	##mplotCorr = map( x - overallmean , mplot[1:4] )
	##print mplotCorr.shape
	
	##mplotStride = rolling_window( mplot[1:4], 200 )
	mplotStride = rolling_window( mplotCorr, 200 )
	##print mplotStride.shape
	##print mplotStride.transpose(1,0).shape
	
	means = numpy.mean( mplotStride, -1 )
	print means.shape
	
	stds = numpy.std( mplotStride, -1 )
	##print stds.shape
	
	##print meanDT.size
	### calculate motion
	speeds = []
	deltaspeeds = []
	offsets = []
	motions = []
	mameans = []
	precision = []
	#mam = mplot[4][0]
	mam = means[0][0]
	avgspeed = []
	#for i in range( 1, mplot[4].size ):
	for i in range( 0, means[0].size - 1  ):
	  #print i
	  #mam = mam*0.99 + mplot[4][i]*0.01
	  mam = mam*0.99 + (means[0][i]-offset)*0.01
	  mameans.append( mam )
	  ##print i
	  #motion_state_machine( mplot[4][i], mplot[5][i], mplot[6][i], mplot[7][i], 5 )
	  #motion_state_machine( mam, mplot[5][i], mplot[6][i], mplot[7][i], 5 )
	  motion_state_machine( mam, stds[0][i], means[1][i], stds[1][i], 10 )
	  if motion_state == 'still':
	    offset = 0.99*offset + 0.01*means[0][i]    
	  #calculate_speed( mplot[4][i], 5 )
	  calculate_speed( mam, mplot[13][i], 5 )
	  #calculate_speed( mam, mplot[13][i], 5 )
	  ##calculate_speed( means[1][i], deltatimes[i] )
	  precision.append( numpy.exp( - stds[0][i] * math.pi/2.0 ) )
	  #precision.append( numpy.exp( -max( mplot[5][i], 0. )*math.pi/2.0 ))
	  speeds.append( speed )
	  offsets.append( offset )
	  deltaspeeds.append( deltaspeed )
	  motions.append( motion_state_index )
	  avgspeed.append( (speed + mplot[16][i])/ 2. );
	  ##print speed
	mspeeds = numpy.asarray( speeds )
	moffsets = numpy.asarray( offsets )
	mdspeeds = numpy.asarray( deltaspeeds )
	mmotion = numpy.asarray( motions )
	mmameans = numpy.asarray( mameans )
	mprecision = numpy.asarray( precision )
	mavgspeed = numpy.asarray( avgspeed )

	

	#plotDT = makeplot( mplot[0], deltatimes, "delta Times" )
	#plotDTm = makeplot( mplot[0], meanDT, "DT mean" )
	#plotDTs = makeplot( mplot[0], stdDT, "DT std" )
	#plotZm = makeplot( mplot[0], means[2], "Z mean" )
	#ContainerHor(plotDT,plotDTm,plotDTs).configure_traits()

	timeaxis = mplot[0] * 0.001

	plotC1 = makeplot( timeaxis, mmotion, "motion calc", -0.5, 3.5 )
	plotC2 = makeplot( timeaxis, mspeeds, "speed calc", -20, 40 )
	plotC3 = makeplot2( timeaxis, mspeeds, "speed calc" )

	plot1 = makeplot( timeaxis, mplot[1], "motion", -0.5, 3.5 )
	plot2 = makeplot2( timeaxis, mplot[2], "speed" )
	
	plot3 = makeplot2( timeaxis, mplot[4], "mean" )
	plot4 = makeplot2( timeaxis, mplot[5], "std" )
	plot5 = makeplot( timeaxis, mplot[6], "side mean", -2.5, 2.5 )
	plot6 = makeplot( timeaxis, mplot[7], "side std", -0.05, 0.6  )
	plot7 = makeplot( timeaxis, mplot[8], "grav mean", -2.5, 2.5 )
	plot8 = makeplot( timeaxis, mplot[9], "grav std", -0.05, 0.6  )
	plot9 = makeplot2( timeaxis, mplot[10], "offset" )
	#plot10 = makeplot( timeaxis, mplot[13], "stilltime", 0, 5 )
	plot11 = makeplot2( timeaxis, mplot[13], "acc forward" )
	plot12 = makeplot2( timeaxis, mplot[14], "acc side" )
	plot13 = makeplot2( timeaxis, mplot[15], "acc grav" )
	plot14 = makeplot2( timeaxis, mplot[16], "gps speed" )
	plot15 = makeplot2( timeaxis, mplot[17], "gps precision" )

	#plot16 = makeplot2( timeaxis[1000:2000], mplot[13][500:2000], "acc forward detail" )
	#plot17 = makeplot2( timeaxis[200:2000], mplot[4][200:2000], "mean forward detail" )
	#plot18 = makeplot2( timeaxis[200:2000], mplot[5][200:2000], "std forward detail" )
	#plot19 = makeplot2( timeaxis[1000:2000], mplot[10][500:1500], "offset detail" )
	#plot20 = makeplot2( timeaxis[200:2000], mmameans[200:2000], "moving average mean" )

	plot21 = makeplot2( timeaxis, mmameans, "moving average mean" )
	#plot22 = makeplot2( timeaxis[200:2000], mplot[7][200:2000], "std side detail" )

	plot23 = makeplot2( timeaxis, mprecision, "precision" )
	plot24 = makeplot2( timeaxis, mdspeeds, "deltaspeed" )
	plot25 = makeplot2( timeaxis, moffsets, "offets calc" )
	
	plot26 = makeplot2( timeaxis, mavgspeed, "average speed (gps + calc)" )

	ContainerHor4(plot4,plot14,plotC3,plot26).configure_traits()

	ContainerHor4(plotC3,plot24,plot21,plot25).configure_traits()

	ContainerHor4(plot14,plotC3,plot3,plot4).configure_traits()
	ContainerHor4(plot14,plotC3,plot21,plot4).configure_traits()
	#ContainerHor4(plotC1,plotC2,plot2,plot3).configure_traits()
	#ContainerHor4(plotC1,plotC2,plot4,plot6).configure_traits()

	#ContainerHor4(plot2,plot3,plot9,plot14).configure_traits()
	#ContainerHor4(plot4,plot2,plot14,plot6).configure_traits()
	#ContainerHor4(plot1,plot2,plot14,plot15).configure_traits()
	#ContainerHor4(plotC1,plotC2,plot1,plot2).configure_traits()

	#ContainerHor4(plot1,plot2,plot3,plot4).configure_traits()
	#ContainerHor4(plot5,plot6,plot7,plot8).configure_traits()
	#ContainerHor4(plot2,plot3,plot9,plot10).configure_traits()
	#ContainerHor4(plot11,plot12,plot13,plot2).configure_traits()
	##ContainerHor4(plotYm,plotYs,plotXm,plotXs).configure_traits()
	##ContainerHor4(plot3,plot4,plot6,plot8).configure_traits()
	##ContainerHor4(plot11,plot3,plot4,plot2).configure_traits()
	#LinePlot( plot4 ).configure_traits();
	#LinePlot( plot6 ).configure_traits();
	##LinePlot( plot8 ).configure_traits();
	##LinePlot( plot11 ).configure_traits();
	##LinePlot( plot13 ).configure_traits();
	##LinePlot( plotC3 ).configure_traits();
	#LinePlot( plot16 ).configure_traits();
	#LinePlot( plot18 ).configure_traits();
	#ContainerHor4(plot17,plot18,plot22,plot20).configure_traits()
	
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
