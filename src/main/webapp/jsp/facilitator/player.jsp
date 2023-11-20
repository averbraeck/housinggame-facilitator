<%@page import="nl.tudelft.simulation.housinggame.facilitator.FacilitatorData"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
  pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
  <head>
    <jsp:include page="head.jsp"></jsp:include>
    <title>Housing Game Facilitator App</title>
    
    <style>
body {
  background-color: white;
  margin: 10px;
}
 
.hg-fac-page {
  background-color: white;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: flex-start;
}

.hg-fac-left {
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: flex-start;
}

.hg-fac-right {
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: flex-start;
}

.hg-fac-header {
}

.hg-fac-row {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
}

.hg-fac-item {
  margin-right: 10px;
}

.hg-grid2-left {
  display: grid;
  row-gap: 4px;
  column-gap: 10px;
  justify-items: start;
  align-items: center;
  justify-content: start;
  align-content: center;
  grid-template-columns: auto auto;
}

.hg-fac-accordion {
  width: 100%;
}

.hg-fac-table td {
  text-align: center;
}

.table > tbody > tr > td, 
.table > tbody > tr > th, 
.table > tfoot > tr > td, 
.table > tfoot > tr > th, 
.table > thead > tr > td, 
.table > thead > tr > th {
  padding: 6px;
}

.table-striped tbody tr:nth-of-type(2n+1) {
  background-color: #cce6ff;
}

.panel-group.pmd-accordion .panel > .panel-heading a {
  padding: 0.5rem 1rem;
  line-height: 1.2rem;
}

.panel-body {
  padding: 1rem;
}
    </style>
  </head>

  <body onload="initPage()">
    <div class="hg-fac-page">
      <div class="hg-fac-left">
        <div class="hg-fac-header">
          <div class="hg-grid2-left">
            <div>
              <img src="images/hg-logo.png" style="width:75px; height:auto;" />
            </div>
            <div>
              <h1>Housing Game Facilitator App</h1>
            </div>
            <div>Game session:</div><div>${facilitatorData.getGameSession().getName() }</div>
            <div>Group / Table:</div><div>${facilitatorData.getGroup().getName() }</div>
            <div>Round:</div><div>${facilitatorData.getRound().getRoundNumber() }</div>
          </div>
          <br />
          <div class="hg-fac-row">
            <div class="hg-fac-item">
              <button disabled="disabled">START GAME</button>
            </div>
            <div class="hg-fac-item">
              <button disabled="disabled">END GAME</button>
            </div>
            <div class="hg-fac-item">
            </div>
          </div>
        </div>
        
        
        <div class="hg-fac-accordion">
			    <div class="panel-group pmd-accordion" id="facilitator-accordion" role="tablist" aria-multiselectable="true" > 
			      
			      <div class="panel panel-default"> 
			        <div class="panel-heading" role="tab" id="heading1">
			          <h4 class="panel-title">
			            <a data-toggle="collapse" data-parent="#facilitator-accordion" href="#collapse1" aria-expanded="true" 
			              aria-controls="collapse1" data-expandable="false">
			              Rounds
			              <i class="material-icons md-dark pmd-sm pmd-accordion-arrow">
			                keyboard_arrow_down
			              </i>
			            </a>
			          </h4>
			        </div>
			        <div id="collapse1" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="heading1">
			          <div class="panel-body">
			            <div class="hg-grid2-left">
                     <div>
                       Allow the minimum <br/>
                       number of players to <br/> 
                       start playing a new <br/>
                       round of the game
                     </div>
                     <div>
                       <button disabled="disabled">START NEW ROUND</button>
                     </div>
			            </div>
			          </div>
			        </div>
			      </div> <!-- item 1: Round -->

             <div class="panel panel-default"> 
               <div class="panel-heading" role="tab" id="heading2">
                 <h4 class="panel-title">
                   <a data-toggle="collapse" data-parent="#facilitator-accordion" href="#collapse2" aria-expanded="false" 
                     aria-controls="collapse2" data-expandable="false">
                     News
                     <i class="material-icons md-dark pmd-sm pmd-accordion-arrow">
                       keyboard_arrow_down
                     </i>
                   </a>
                 </h4>
               </div>
               <div id="collapse2" class="panel-collapse collapse" role="tabpanel" aria-labelledby="heading2">
                 <div class="panel-body">
                   <div class="hg-grid2-left">
                     <div>
                       Show the available houses <br/>
                       to the players on the table.<br/>
                       Go to your house overview. <br/>
                       Read the news and press the<br/> 
                       'Announce news' button so that <br/>
                       players explore the house list <br/>
                       on their phone
                     </div>
                     <div>
                       <button disabled="disabled">ANNOUNCE NEWS</button>
                     </div>
                   </div>
                 </div>
               </div>
             </div> <!-- item 2: News -->

             <div class="panel panel-default"> 
               <div class="panel-heading" role="tab" id="heading3">
                 <h4 class="panel-title">
                   <a data-toggle="collapse" data-parent="#facilitator-accordion" href="#collapse3" aria-expanded="false" 
                     aria-controls="collapse3" data-expandable="false">
                     House market
                     <i class="material-icons md-dark pmd-sm pmd-accordion-arrow">
                       keyboard_arrow_down
                     </i>
                   </a>
                 </h4>
               </div>
               <div id="collapse3" class="panel-collapse collapse" role="tabpanel" aria-labelledby="heading3">
                 <div class="panel-body">
                   <div class="hg-grid2-left">
                     <div>
                       Ensure every player puts <br/>
                       the pawn on the house they<br/> 
                       want to buy. If multiple <br/>
                       players want the same <br/>
                       house, give them the paper <br/>
                       card to bid. <br/>
                       Edit 'buy prices' as needed <br/>
                       before allowing house buy
                     </div>
                     <div>
                       <button disabled="disabled">ALLOW HOUSE BUY</button>
                     </div>
                   </div>
                 </div>
               </div>
             </div> <!-- item 3: House market -->

             <div class="panel panel-default"> 
               <div class="panel-heading" role="tab" id="heading4">
                 <h4 class="panel-title">
                   <a data-toggle="collapse" data-parent="#facilitator-accordion" href="#collapse4" aria-expanded="false" 
                     aria-controls="collapse4" data-expandable="false">
                     House improvements
                     <i class="material-icons md-dark pmd-sm pmd-accordion-arrow">
                       keyboard_arrow_down
                     </i>
                   </a>
                 </h4>
               </div>
               <div id="collapse4" class="panel-collapse collapse" role="tabpanel" aria-labelledby="heading4">
                 <div class="panel-body">
                   <div class="hg-grid2-left">
                     <div>
                       When all players have chosen <br/>
                       a house and agreed on the <br/>
                       prices, save the buy prices <br/>
                       and allow the house buy
                     </div>
                     <div>
                       <button disabled="disabled">HOUSE MEASURES</button>
                     </div>
                   </div>
                   <div class="hg-grid2-left">
                     <div>
                       When all players buy a house, <br/>
                       calculate taxes so that players <br/>
                       can move into their houses
                     </div>
                     <div>
                       <button disabled="disabled">CALCULATE TAXES</button>
                     </div>
                   </div>
                 </div>
               </div>
             </div> <!-- item 4: House improvements -->

            <div class="panel panel-default"> 
               <div class="panel-heading" role="tab" id="heading5">
                 <h4 class="panel-title">
                   <a data-toggle="collapse" data-parent="#facilitator-accordion" href="#collapse5" aria-expanded="false" 
                     aria-controls="collapse5" data-expandable="false">
                     Player perceptions
                     <i class="material-icons md-dark pmd-sm pmd-accordion-arrow">
                       keyboard_arrow_down
                     </i>
                   </a>
                 </h4>
               </div>
               <div id="collapse5" class="panel-collapse collapse" role="tabpanel" aria-labelledby="heading5">
                 <div class="panel-body">
                   <div class="hg-grid2-left">
                     <div>
                       Invite all players to<br/>
                       enter the answers to the<br/>
                       survey questions.
                     </div>
                     <div>
                       <button disabled="disabled">OPEN SURVEY</button>
                     </div>
                   </div>
                 </div>
               </div>
             </div> <!-- item 5: Player perceptions -->

            <div class="panel panel-default"> 
               <div class="panel-heading" role="tab" id="heading6">
                 <h4 class="panel-title">
                   <a data-toggle="collapse" data-parent="#facilitator-accordion" href="#collapse6" aria-expanded="false" 
                     aria-controls="collapse6" data-expandable="false">
                     Floods
                     <i class="material-icons md-dark pmd-sm pmd-accordion-arrow">
                       keyboard_arrow_down
                     </i>
                   </a>
                 </h4>
               </div>
               <div id="collapse6" class="panel-collapse collapse" role="tabpanel" aria-labelledby="heading6">
                 <div class="panel-body">
                   <div class="hg-grid2-left">
                     <div>
                       Roll the dice for pluvial<br/>
                       and fluvial impacts, and<br/>
                       enter the data to the right<br/>
                     </div>
                     <div>
                       <label for="pluvial">Pluvial dice roll:</label><br>
                       <input type="number" id="pluvial" name="pluvial" disabled="disabled"><br>
                       <label for="fluvial">Fluvial dice roll:</label><br>
                       <input type="number" id="fluvial" name="fluvial" disabled="disabled"><br>
                       <button disabled="disabled">ENTER DICE ROLLS</button>
                     </div>
                   </div>
                 </div>
               </div>
             </div> <!-- item 6: Floods -->

            <div class="panel panel-default"> 
               <div class="panel-heading" role="tab" id="heading7">
                 <h4 class="panel-title">
                   <a data-toggle="collapse" data-parent="#facilitator-accordion" href="#collapse7" aria-expanded="false" 
                     aria-controls="collapse7" data-expandable="false">
                     House market
                     <i class="material-icons md-dark pmd-sm pmd-accordion-arrow">
                       keyboard_arrow_down
                     </i>
                   </a>
                 </h4>
               </div>
               <div id="collapse7" class="panel-collapse collapse" role="tabpanel" aria-labelledby="heading7">
                 <div class="panel-body">
                   <div class="hg-grid2-left">
                     <div>
                       Press the button to<br/>
                       show the players the <br/>
                       effects of the rain/flood<br/>
                       on their houses
                     </div>
                     <div>
                       <button disabled="disabled">SHOW DAMAGE</button>
                     </div>
                   </div>
                 </div>
               </div>
             </div> <!-- item 7: House buy -->

           </div> <!-- pmd-accordion -->

        </div> <!-- hg-fac-accordion -->
      </div>
      
      <div style="width: 36px;"></div>
      
      <div class="hg-fac-right">
        <div class="hg-fac-menu">
          <div class="hg-fac-row">
            <div class="hg-fac-item">
              <form action="/housinggame-facilitator/jsp/facilitator/player.jsp" method="post">
                <div class="hg-button">
                  <input type="submit" value='Player overview' class="btn btn-primary" />
                </div>
              </form>
            </div>
            <div class="hg-fac-item">
              <form action="/housinggame-facilitator/jsp/facilitator/house.jsp" method="post">
                <div class="hg-button">
                  <input type="submit" value='House overview' class="btn" />
                </div>
              </form>
            </div>
            <div class="hg-fac-item">
              <form action="/housinggame-facilitator/jsp/facilitator/flood.jsp" method="post">
                <div class="hg-button">
                  <input type="submit" value='Flood overview' class="btn" />
                </div>
              </form>
            </div>
            <div class="hg-fac-item">
              <form action="/housinggame-facilitator/jsp/facilitator/news.jsp" method="post">
                <div class="hg-button">
                  <input type="submit" value='News overview' class="btn" />
                </div>
              </form>
            </div>
            <div class="hg-fac-item">
              <form action="/housinggame-facilitator/jsp/facilitator/login.jsp" method="post">
					      <div class="hg-button">
					        <input type="submit" value='logout' class="btn" />
					      </div>
					    </form>
            </div>
          </div>
        </div>
        <div class="hg-fac-table">
          <table class="pmd table table-striped" style="text-align:center;">
             ${facilitatorData.getContentHtml("player/playerStateTable") }
          </table>
        </div>

        <div class="hg-fac-table">
           <table class="pmd table table-striped" style="text-align:center;">
             ${facilitatorData.getContentHtml("player/playerBudgetTable") }
           </table>
        </div>
      </div>
    </div>
  </body>

</html>