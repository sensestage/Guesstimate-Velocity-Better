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
  
class LinePlot(HasTraits):
    plot = Instance(Plot)
    traits_view = View(
        Item('plot',editor=ComponentEditor(), show_label=False),
        width=500, height=500, resizable=True, title="Chaco Plot")

    def __init__(self, plot ):
        super(LinePlot, self).__init__()
        
        self.plot = plot


if __name__ == "__main__":

  parser = optparse.OptionParser(description='Plot the log.')
  parser.add_option('-f','--file', action='store', type="string", dest="filename",default="",
		  help='the name of the log file to plot [default:%s]'% '')

  (options,args) = parser.parse_args()

  #x = numpy.asarray( range( 0, 500 ) ) / 10.
  #y = []
  #for i in x:
    #y.append( 20 * ( 1 - math.exp( i ) ) )
  #y = numpy.exp( -x*math.pi/10. )
  #y2 = pow( 20 - x, 2 )/400.
  #y2 = 1 - numpy.exp( -x*math.pi/50. )
  #y = numpy.asarray( y )

  x = numpy.asarray( range( 0, 4000 ) ) / 1000.
  y2 = numpy.exp( -x*math.pi/3. )
  #y = numpy.asarray( y )

  #plot = makeplot( x, y, "limiter" )
  plot2 = makeplot( x, y2, "precision" )
  #print y
  #LinePlot( plot ).configure_traits();
  LinePlot( plot2 ).configure_traits();
  