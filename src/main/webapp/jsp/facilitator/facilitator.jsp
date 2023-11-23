<%@page
	import="nl.tudelft.simulation.housinggame.facilitator.FacilitatorData"%>
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
  flex-wrap: nowrap;
  justify-content: flex-start;
  align-items: flex-start;
}

.hg-fac-left {
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
  flex-shrink: 0; justify-content : flex-start;
  align-items: flex-start;
  justify-content: flex-start;
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

.hg-grid2-left-right {
  display: grid;
  row-gap: 4px;
  column-gap: 10px;
  justify-items: start;
  align-items: center;
  justify-content: space-between;
  align-content: center;
  grid-template-columns: auto auto;
}

.hg-fac-accordion {
  width: 100%;
}

.hg-fac-table td {
  text-align: center;
}

.table>tbody>tr>td, .table>tbody>tr>th, .table>tfoot>tr>td, .table>tfoot>tr>th,
  .table>thead>tr>td, .table>thead>tr>th {
  padding: 6px;
}

.table-striped tbody tr:nth-of-type(2n+1) {
  background-color: #cce6ff;
}

.panel-group.pmd-accordion .panel>.panel-heading a {
  padding: 0.5rem 1rem;
  line-height: 1.2rem;
}

.panel-body {
  padding: 1rem;
}

.btn-active {
  disabled: false;
  cursor: pointer;
}

.btn-inactive {
  disabled: true !important;
  cursor: not-allowed !important;
}
</style>
</head>

<body onload="initPage()">
	<div class="hg-fac-page">
		<div class="hg-fac-left">
			<div class="hg-fac-header">
				<div class="hg-grid2-left">
					<div>
						<img src="images/hg-logo.png" style="width: 75px; height: auto;" />
					</div>
					<div>
						<h1>Housing Game Facilitator App</h1>
					</div>
					<div>Game session:</div>
					<div>${facilitatorData.getGameSession().getName() }</div>
					<div>Group / Table:</div>
					<div>${facilitatorData.getGroup().getName() }</div>
					<div>Round:</div>
					<div>${facilitatorData.getCurrentRoundNumber() }</div>
				</div>
				<br />
			</div>


			<div class="hg-fac-accordion">
				<div class="panel-group pmd-accordion" id="facilitator-accordion"
					role="tablist" aria-multiselectable="true">

					<div class="panel panel-default">
						<div class="panel-heading" role="tab" id="heading1">
							<h4 class="panel-title">
								<a data-toggle="collapse" data-parent="#facilitator-accordion"
									href="#collapse1" aria-expanded="true"
									aria-controls="collapse1" data-expandable="false"> Rounds <i
									class="material-icons md-dark pmd-sm pmd-accordion-arrow">
										keyboard_arrow_down </i>
								</a>
							</h4>
						</div>
						<div id="collapse1"
							class='panel-collapse collapse ${facilitatorData.getContentHtml("accordion/new-round") }'
							role="tabpanel" aria-labelledby="heading1">
							<div class="panel-body">
								<div class="hg-grid2-left-right">
									<div>
										Allow the minimum <br /> number of players to <br /> start
										playing a new <br /> round of the game
									</div>
									<div>
										<form action="/housinggame-facilitator/facilitator"
											method="post">
											<input type="hidden" name="button" value="new-round" />
											<div class="hg-button">
												<input type="submit" value="START NEW ROUND"
													class='${facilitatorData.getContentHtml("button/new-round") }' />
											</div>
										</form>
									</div>
								</div>
							</div>
						</div>
					</div>
					<!-- item 1: Round -->

					<div class="panel panel-default">
						<div class="panel-heading" role="tab" id="heading2">
							<h4 class="panel-title">
								<a data-toggle="collapse" data-parent="#facilitator-accordion"
									href="#collapse2" aria-expanded="false"
									aria-controls="collapse2" data-expandable="false"> News <i
									class="material-icons md-dark pmd-sm pmd-accordion-arrow">
										keyboard_arrow_down </i>
								</a>
							</h4>
						</div>
						<div id="collapse2"
							class='panel-collapse collapse ${facilitatorData.getContentHtml("accordion/announce-news") }'
							role="tabpanel" aria-labelledby="heading2">
							<div class="panel-body">
								<div class="hg-grid2-left-right">
									<div>
										Show the available houses <br /> to the players on the table.<br />
										Go to your house overview. <br /> Read the news and press the<br />
										'Announce news' button so that <br /> players explore the news
										<br /> on their phone
									</div>
									<form action="/housinggame-facilitator/facilitator"
										method="post">
										<input type="hidden" name="button" value="announce-news" />
										<div class="hg-button">
											<input type="submit" value="ANNOUNCE NEWS"
												class='${facilitatorData.getContentHtml("button/announce-news") }' />
										</div>
									</form>
								</div>
							</div>
						</div>
					</div>
					<!-- item 2: News -->

					<div class="panel panel-default">
						<div class="panel-heading" role="tab" id="heading3">
							<h4 class="panel-title">
								<a data-toggle="collapse" data-parent="#facilitator-accordion"
									href="#collapse3" aria-expanded="false"
									aria-controls="collapse3" data-expandable="false"> House
									market <i
									class="material-icons md-dark pmd-sm pmd-accordion-arrow">
										keyboard_arrow_down </i>
								</a>
							</h4>
						</div>
						<div id="collapse3"
							class='panel-collapse collapse ${facilitatorData.getContentHtml("accordion/houses-taxes") }'
							role="tabpanel" aria-labelledby="heading3">
							<div class="panel-body">
								<div class="hg-grid2-left-right">
									<div>
										Ensure every player puts <br /> the pawn on the house they<br />
										want to buy. If multiple <br /> players want the same <br />
										house, give them the paper <br /> card to bid. <br /> Edit
										'buy prices' as needed <br /> before allowing house buy
									</div>
									<div
										style="display: flex; flex-direction: column; justify-content: space-evenly;">
										<form action="/housinggame-facilitator/facilitator"
											method="post" style="margin-top: 10px; margin-bottom: 10px;">
											<input type="hidden" name="button" value="show-houses" />
											<div class="hg-button">
												<input type="submit" value="SHOW HOUSES"
													class='${facilitatorData.getContentHtml("button/show-houses") }' />
											</div>
										</form>
										<form action="/housinggame-facilitator/facilitator"
											method="post" style="margin-top: 10px; margin-bottom: 10px;">
											<input type="hidden" name="button" value="assign-houses" />
											<div class="hg-button">
												<input type="submit" value="ASSIGN HOUSES"
													class='${facilitatorData.getContentHtml("button/assign-houses") }' />
											</div>
										</form>
										<form action="/housinggame-facilitator/facilitator"
											method="post" style="margin-top: 10px; margin-bottom: 10px;">
											<input type="hidden" name="button" value="calculate-taxes" />
											<div class="hg-button">
												<input type="submit" value="CALCULATE TAXES"
													class='${facilitatorData.getContentHtml("button/calculate-taxes") }' />
											</div>
										</form>
									</div>
								</div>
							</div>
						</div>
					</div>
					<!-- item 3: House market -->

					<div class="panel panel-default">
						<div class="panel-heading" role="tab" id="heading4">
							<h4 class="panel-title">
								<a data-toggle="collapse" data-parent="#facilitator-accordion"
									href="#collapse4" aria-expanded="false"
									aria-controls="collapse4" data-expandable="false"> House
									improvements <i
									class="material-icons md-dark pmd-sm pmd-accordion-arrow">
										keyboard_arrow_down </i>
								</a>
							</h4>
						</div>
						<div id="collapse4"
							class='panel-collapse collapse ${facilitatorData.getContentHtml("accordion/allow-improvements") }'
							role="tabpanel" aria-labelledby="heading4">
							<div class="panel-body">
								<div class="hg-grid2-left-right">
									<div>
										When all players have chosen <br /> a house and agreed on the
										<br /> prices, save the buy prices <br /> and allow the house
										buy
									</div>
									<form action="/housinggame-facilitator/facilitator"
										method="post">
										<input type="hidden" name="button" value="allow-improvements" />
										<div class="hg-button">
											<input type="submit" value="ALLOW IMPROVEMENTS"
												class='${facilitatorData.getContentHtml("button/allow-improvements") }' />
										</div>
									</form>
								</div>
							</div>
						</div>
					</div>
					<!-- item 4: House improvements -->

					<div class="panel panel-default">
						<div class="panel-heading" role="tab" id="heading5">
							<h4 class="panel-title">
								<a data-toggle="collapse" data-parent="#facilitator-accordion"
									href="#collapse5" aria-expanded="false"
									aria-controls="collapse5" data-expandable="false"> Player
									perceptions <i
									class="material-icons md-dark pmd-sm pmd-accordion-arrow">
										keyboard_arrow_down </i>
								</a>
							</h4>
						</div>
						<div id="collapse5"
							class='panel-collapse collapse ${facilitatorData.getContentHtml("accordion/ask-perceptions") }'
							role="tabpanel" aria-labelledby="heading5">
							<div class="panel-body">
								<div class="hg-grid2-left-right">
									<div>
										Invite all players to<br /> enter the answers to the<br />
										survey questions.
									</div>
									<form action="/housinggame-facilitator/facilitator"
										method="post">
										<input type="hidden" name="button" value="ask-perceptions" />
										<div class="hg-button">
											<input type="submit" value="ASK PERCEPTIONS"
												class='${facilitatorData.getContentHtml("button/ask-perceptions") }' />
										</div>
									</form>
								</div>
							</div>
						</div>
					</div>
					<!-- item 5: Player perceptions -->

					<div class="panel panel-default">
						<div class="panel-heading" role="tab" id="heading6">
							<h4 class="panel-title">
								<a data-toggle="collapse" data-parent="#facilitator-accordion"
									href="#collapse6" aria-expanded="false"
									aria-controls="collapse6" data-expandable="false"> Floods <i
									class="material-icons md-dark pmd-sm pmd-accordion-arrow">
										keyboard_arrow_down </i>
								</a>
							</h4>
						</div>
						<div id="collapse6"
							class='panel-collapse collapse ${facilitatorData.getContentHtml("accordion/roll-dice") }'
							role="tabpanel" aria-labelledby="heading6">
							<div class="panel-body">
								<div class="hg-grid2-left-right">
									<div>
										Roll the dice for pluvial<br /> and fluvial impacts, and<br />
										enter the data to the right<br /> Press the button to<br />
										show the players the <br /> effects of the rain/flood<br /> on
										their houses
									</div>
									<form action="/housinggame-facilitator/facilitator"
										method="post">
										<input type="hidden" name="button" value="roll-dice" /> <label
											for="pluvial">Pluvial dice roll:</label><br> <input
											type="number" id="pluvial" name="pluvial"><br> <label
											for="fluvial">Fluvial dice roll:</label><br> <input
											type="number" id="fluvial" name="fluvial"><br>
										<div class="hg-button">
											<input type="submit" value="ROLL DICE"
												class='${facilitatorData.getContentHtml("button/roll-dice") }' />
										</div>
									</form>
								</div>
							</div>
						</div>
					</div>
					<!-- item 6: Floods -->

					<div class="panel panel-default">
						<div class="panel-heading" role="tab" id="heading7">
							<h4 class="panel-title">
								<a data-toggle="collapse" data-parent="#facilitator-accordion"
									href="#collapse7" aria-expanded="false"
									aria-controls="collapse7" data-expandable="false"> Damage <i
									class="material-icons md-dark pmd-sm pmd-accordion-arrow">
										keyboard_arrow_down </i>
								</a>
							</h4>
						</div>
						<div id="collapse7"
							class='panel-collapse collapse ${facilitatorData.getContentHtml("accordion/show-summary") }'
							role="tabpanel" aria-labelledby="heading7">
							<div class="panel-body">
								<div class="hg-grid2-left-right">
									<div>
										Press the button to<br /> allow the players to <br /> see the
										summary screen<br /> for the round
									</div>
									<form action="/housinggame-facilitator/facilitator"
										method="post">
										<input type="hidden" name="button" value="show-summary" />
										<div class="hg-button">
											<input type="submit" value="SHOW SUMMARY"
												class='${facilitatorData.getContentHtml("button/show-summary") }' />
										</div>
									</form>
								</div>
							</div>
						</div>
					</div>
					<!-- item 7: House buy -->

				</div>
				<!-- pmd-accordion -->

			</div>
			<!-- hg-fac-accordion -->
		</div>

		<div style="width: 48px; min-width: 48px;"></div>

		<div class="hg-fac-right">
			<div class="hg-fac-menu">
				<div class="hg-fac-row">
					<div class="hg-fac-item">
						<form action="/housinggame-facilitator/facilitator" method="post">
							<input type="hidden" name="menu" value="Player" />
							<div class="hg-button">
								<input type="submit" value='Player overview'
									class='${facilitatorData.getContentHtml("menuPlayer")}' />
							</div>
						</form>
					</div>
					<div class="hg-fac-item">
						<form action="/housinggame-facilitator/facilitator" method="post">
							<input type="hidden" name="menu" value="House" />
							<div class="hg-button">
								<input type="submit" value='House overview'
									class='${facilitatorData.getContentHtml("menuHouse")}' />
							</div>
						</form>
					</div>
					<div class="hg-fac-item">
						<form action="/housinggame-facilitator/facilitator" method="post">
							<input type="hidden" name="menu" value="Flood" />
							<div class="hg-button">
								<input type="submit" value='Flood overview'
									class='${facilitatorData.getContentHtml("menuFlood")}' />
							</div>
						</form>
					</div>
					<div class="hg-fac-item">
						<form action="/housinggame-facilitator/facilitator" method="post">
							<input type="hidden" name="menu" value="News" />
							<div class="hg-button">
								<input type="submit" value='News overview'
									class='${facilitatorData.getContentHtml("menuNews")}' />
							</div>
						</form>
					</div>
					<div class="hg-fac-item">
						<form action="/housinggame-facilitator/jsp/facilitator/login.jsp"
							method="post">
							<div class="hg-button">
								<input type="submit" value='logout' class="btn" />
							</div>
						</form>
					</div>
				</div>
			</div>

      <div id="facilitator-tables">
        ${facilitatorData.getContentHtml("facilitator/tables") }
      </div>

			<!--  buying and selling houses, will be moved to a variable -->

			<div class="hg-grid2-left">
				<div>
					<b>Buying of house by player</b>
					<form action="/housinggame-facilitator/facilitator" method="post">
						<input type="hidden" name="house-management" value="buy-house" />
						<div style="display: flex; flex-direction: column;">
							<div>
								<label for="buy-house">House for player to buy</label> <select
									name="house" id="buy-house">
									<option value=""></option>
									<option value="U01">U01</option>
									<option value="D01">D01</option>
								</select>
							</div>
							<div>
								<label for="buy-player">Player who buys</label> <select
									name="player" id="buy-player">
									<option value=""></option>
									<option value="t1p1">t1p1</option>
									<option value="t1p2">t1p2</option>
								</select>
							</div>
							<div>
								<label for="buy-price">Adjusted buying price</label> <input
									type="number" id="buy-price" name="buy-price" value="">
							</div>
							<br />
							<div class="hg-button">
								<input type="submit" value='Sell house to player'
									class="btn btn-primary" />
							</div>
						</div>
					</form>
				</div>
				<div>
					<b>Selling of house to the bank</b>
					<form action="/housinggame-facilitator/facilitator" method="post">
						<input type="hidden" name="house-management" value="sell-house" />
						<div style="display: flex; flex-direction: column;">
							<div>
								<label for="sell-house">House player sells to bank</label> <select
									name="house" id="sell-house">
									<option value=""></option>
									<option value="U01">U01</option>
									<option value="D01">D01</option>
								</select>
							</div>
							<div>
								<label for="sell-price">Adjusted selling price</label> <input
									type="number" id="sell-price" name="sell-price" value="">
							</div>
							<div>
								<label for="sell-reason">Reason for selling house</label> <input
									type="text" id="sell-reason" name="sell-reason" value="">
							</div>
							<br />
							<div class="hg-button">
								<input type="submit" value='Sell house to the bank'
									class="btn btn-primary" />
							</div>
						</div>
					</form>
				</div>
			</div>

		</div>
	</div>

	${facilitatorData.getModalWindowHtml()}

  <script type="text/javascript">
    $(document).ready(function() {
      reloadTables();
    });
    function reloadTables() {
      $.post("/housinggame-facilitator/reload-tables", {reloadTables: 'true'},
        function(data, status) {
    	      var tableDiv = document.getElementById("facilitator-tables");
    	      tableDiv.innerHTML = data;
            setTimeout(reloadTables, 5000);
          });
    };
  </script>
  
</body>

</html>