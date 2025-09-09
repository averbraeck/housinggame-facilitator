<%@page import="nl.tudelft.simulation.housinggame.facilitator.SessionUtils"%>
<%@page import="nl.tudelft.simulation.housinggame.facilitator.FacilitatorData"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
 pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Housing Game Facilitator Login</title>

<!--  favicon -->
<link rel="shortcut icon" href="/housinggame-facilitator/favicon.ico" type="image/x-icon">

<style>
html, body {
  font-family: Roboto, Arial, Helvetica, sans-serif;
}

.hg-login-page {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  position: absolute;
  top: 20px;
  bottom: 20px;
  left: 20px;
  right: 20px;
  min-height: 0px;
}

.hg-header, .hg-body, .hg-footer {
  flex-shrink: 0;
}

.hg-login-header {
  background: navy;
  padding: 5px;
  height: 50px;
  text-align: left;
  line-height: 50px;
  color: white;
  font-weight: bold;
  font-size: 2em;
  border-radius: 10px;
  margin-bottom: 20px;
}

.hg-login-header-right {
  position: absolute;
  background: white;
  margin: 2px;
  height: 46px;
  width: 300px;
  right: 20px;
  border-radius: 20px;
  flex-direction: row;
}

.hg-login-header-right > img {
  margin-left: 20px;
  margin-top: 3px;
  height: 40px;
  width: auto;
}

.hg-login-body {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-height: 0;
  height: calc(100vh - 200px);
  width: 600px;
  margin-left: calc(50vw - 300px);
  padding-right: 20px;
  overflow-y: auto;
  overflow-x: hidden;
}

.hg-login-footer {
  background: navy;
  padding: 5px;
  height: 50px;
  line-height: 50px;
  color: white;
  font-weight: bold;
  font-size: 1em;
  text-align: right;
  border-radius: 10px;
  margin-top: 20px;
}

.hg-login-top-message {
  font-style: normal;
  font-size: 1em;
  margin-bottom: 20px;
}

.hg-login-top-message > h1 {
  font-style: normal;
  font-weight: bold;
  font-size: 1.8em;
  text-align: left;
  color: orange;
}

.hg-login-top-message > p {
  text-align: justify;
  line-height: 1.2;
}

.hg-login-bottom-message {
  font-style: normal;
  font-size: 0.8em;
  margin-top: 20px;
}

.hg-login-bottom-message > p {
  text-align: justify;
  line-height: 1.2;
}

.hg-login {
  width: 595px;
  border: 3px solid orange;
  border-radius: 10px;
  padding-top: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hg-login-button {
  display: block;
  width: 595px;
  border: none;
  background-color: orange;
  padding: 14px 28px;
  cursor: pointer;
  text-align: center;
  font-size: 1.3em;
  font-weight: bold;
}

.hg-logo-right {
  display: block;
}

.hg-logo-right > img {
  width: 150px;
  float: right;
  padding-top: 40px;
}

</style>
</head>

<body>

<%
  if(session.getAttribute("facilitatorData") == null) {
    FacilitatorData facilitatorData = SessionUtils.getData(session);
  }
%>

  <div class="hg-login-page">
    <div class="hg-login-header">
      <div class="hg-login-header-right">
        <img src="images/tudelft.png" />
        <span style="font-size: 12px; padding-left: 20px; position:relative; top:-4px; color:black;">v1.7.1</span>
      </div>
    </div>
  
    <div class="hg-login-body">
    
      <div class="hg-login-top-message">
        <div class="hg-logo-right">
          <img src="images/hg-logo.png" />
        </div>
        <h1>Housing Game Facilitator App</h1> 
        <p>The following functions are available:</p>
        <ul>
          <li>Initialize play for the table</li>
          <li>Advance the group to a next round</li>
          <li>Read the news for the round</li>
          <li>Enter the results of fluvial and pluvial events</li>
          <li>Follow the players' progress</li>
        </ul>
      </div>

      <div class="hg-login">
        <form action="/housinggame-facilitator/login" method="post">
         <table>
           <tr>
             <td width="60px">&nbsp;</td>
             <td>Session &nbsp; </td>
             <td>
               <select name="gamesession">
                 ${facilitatorData.getValidSessionOptions() }
               </select>
             </td>
           </tr>
           <tr>
             <td width="60px">&nbsp;</td>
             <td>User name &nbsp; </td>
             <td><input type="text" name="username" /></td>
           </tr>
           <tr>
             <td width="150px">&nbsp;</td>
             <td>Password &nbsp; </td>
             <td><input type="password" name="password" /></td>
           </tr>
         </table>
         <br/>
         <span>
           <input type="submit" value="FACILITATOR LOGIN" class="hg-login-button" />
         </span>
        </form>
      </div>
  
      <div class="hg-login-bottom-message">
        <p> 
          If you have any questions about the game or the research, feel free to contact 
          Juliette Cortes-Arevalo at TU Delft (<a href="mailto:v.j.cortesarevalo@tudelft.nl">v.j.cortesarevalo@tudelft.nl</a>).
        </p>
      </div>
  
    </div>
    
    <div class="hg-login-footer">
      <!-- logo's at top right -->
    </div>
    
  </div>      
</body>
</html>