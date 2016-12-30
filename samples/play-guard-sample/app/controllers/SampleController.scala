package controllers

import com.digitaltangible.playguard.ActionRateLimiter
import play.api.mvc._

/**
  * Test dummy app for rate limit actions
  */
class SampleController(rlActionBuilder: ActionRateLimiter) extends Controller {

  def index: Action[AnyContent] = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  // allow 3 requests immediately and get a new token every 5 seconds
  private val ipRateLimitedAction = rlActionBuilder.ipRateLimiterAction(rlActionBuilder.RateLimiter(3, 1f / 5, "test limit by IP address")) {
    implicit r: RequestHeader => BadRequest( s"""rate limit for ${r.remoteAddress} exceeded""")
  }

  def limitedByIp: Action[AnyContent] = ipRateLimitedAction {
    Ok("limited by IP")
  }


  // allow 4 requests immediately and get a new token every 15 seconds
  private val tokenRateLimitedAction = rlActionBuilder.keyRateLimiterAction(rlActionBuilder.RateLimiter(4, 1f / 15, "test by token")) _

  def limitedByKey(key: String): Action[AnyContent] = tokenRateLimitedAction(_ => BadRequest( s"""rate limit for '$key' exceeded"""))(key) {
    Ok("limited by token")
  }


  // allow 2 failures immediately and get a new token every 10 seconds
  private val failRateLimited = rlActionBuilder.failureRateLimiterAction(2, 1f / 10, {
    implicit r: RequestHeader => BadRequest("failure rate exceeded")
  }, "test failure rate limit")

  def failureLimitedByIp(fail: Boolean): Action[AnyContent] = failRateLimited {
    if (fail) BadRequest("failed")
    else Ok("Ok")
  }

  // combine tokenRateLimited and failRateLimited
  def limitByKeyAndFailureLimitedByIp(key: String, fail: Boolean): Action[AnyContent] =
    (tokenRateLimitedAction(_ => BadRequest( s"""rate limit for '$key' exceeded"""))(key) andThen failRateLimited) {

      if (fail) BadRequest("failed")
      else Ok("Ok")
    }
}