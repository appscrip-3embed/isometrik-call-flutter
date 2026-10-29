import '../api/isometrik_api_error.dart';
import '../models/models.dart';

/// Optional host meeting REST.
///
/// When set on [IsometrikCallSdk.meetingHostDelegate], accept / reject / leave /
/// end / startPublishing skip the default Isometrik `meetings/v1/*` APIs.
/// Any host can implement this. The SDK does not interpret host-specific ids.
abstract class IsometrikMeetingHostDelegate {
  Future<IsometrikResult<IsometrikMeeting>> acceptCall({
    required String meetingId,
    required String deviceId,
  });

  Future<IsometrikResult<IsometrikMeeting>> rejectCall({
    required String meetingId,
    required String deviceId,
  });

  /// Participant hang-up after accept.
  Future<IsometrikResult<Null>> leaveMeeting({
    required String meetingId,
  });

  /// Creator / outbound end. Hosts whose create/accept already returned RTC
  /// can implement this against their own meeting API.
  Future<IsometrikResult<Null>> endMeeting({
    required String meetingId,
  });

  /// LiveKit is connected. Hosts that already received RTC from create/accept
  /// can return success without calling an Isometrik publish API.
  Future<IsometrikResult<IsometrikMeeting>> startPublishing({
    required String meetingId,
    required String deviceId,
  });
}
