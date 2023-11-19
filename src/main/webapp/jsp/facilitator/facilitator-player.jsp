<%@page import="nl.tudelft.simulation.housinggame.facilitator.FacilitatorData"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
  pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
  <head>
    <jsp:include page="head.jsp"></jsp:include>
    <title>Housing Game Facilitator App</title>
  </head>

  <body onload="initPage()">
    <div class="hg-page">
      <div class="hg-header">
        <span class="hg-game-heading">Housing Game</span>
        <span class="hg-slogan">Game Facilitator App</span>
      </div>
      <div class="hg-header-right">
        <img src="images/tudelft.png" />
        <p><a href="/housinggame-facilitator">LOGOUT</a></p>
        <span style="font-size: 12px; padding-left: 20px; position:relative; top:-4px; color:black;">v1.3.0</span>
      </div>
      <div class="hg-header-game-user">
        <p>&nbsp;</p>
        <p>User:&nbsp;&nbsp;&nbsp; ${facilitatorData.getUser().getUsername()}</p>
      </div>

      <div class="hg-body">
      
        <div class="hg-facilitator-menu">
          ${facilitatorData.getTopMenu()}
        </div>
        <div class="hg-facilitator" id="hg-facilitator">
          ${facilitatorData.getContentHtml()}
        </div>
        
      </div> <!-- hg-body -->
      
    </div> <!-- hg-page -->
    
    <!-- modal window for the client information within an order -->
    
    ${facilitatorData.getModalWindowHtml()}

    <form id="clickForm" action="/housinggame-facilitator/facilitator" method="POST" style="display:none;">
      <input id="click" type="hidden" name="click" value="tobefilled" />
      <input id="recordNr" type="hidden" name="recordNr" value="0" />
    </form>

  </body>

</html>